package com.ismartcoding.plain.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.SystemClock
import com.ismartcoding.plain.lib.logcat.LogCat

/**
 * Streaming touch injection for TOUCH_DOWN / TOUCH_MOVE / TOUCH_UP, built
 * around this platform's dispatchGesture contract, measured on-device
 * (plain-cast docs/touch-low-latency-design.md §7):
 *
 *  - a fresh stroke with willContinue=true is accepted and keeps the touch
 *    down after its timeline ends (a "dangling" stroke);
 *  - a continuation with willContinue=true is CANCELLED by the system in
 *    every timing combination — streaming chains are unusable here;
 *  - a terminal continuation (willContinue=false) is accepted early,
 *    exactly on completion, and hundreds of ms past the stroke's end.
 *
 * Architecture ("dangling stroke + terminal release"):
 *  - DOWN dispatches one long dangling stroke — the touch is down at once.
 *  - While movement stays within tap slop nothing else is dispatched: the
 *    press is a single continuous touch, released by ONE terminal
 *    continuation carrying the buffered path — taps have exact duration
 *    and can never degrade into long-presses.
 *  - Once movement exceeds tap slop the gesture is a drag: buffered samples
 *    are flushed as batched fresh strokes (each replacing the previous
 *    touch) at a ~30ms cadence, each replaying the real trajectory and speed.
 *  - Edge-originated drags are held instead and classified at UP: inward
 *    swipes fire their system action (bottom→home, left/right→back,
 *    top→notifications) — injected raw touches never trigger SystemUI edge
 *    navigation themselves.
 *  - UP releases the current lineage with a terminal continuation.
 *  - A periodic refresh re-arms the dangling stroke for long holds, and a
 *    stale watchdog force-releases when the client disappears mid-press.
 *
 * One active pointer at a time: dispatchGesture serializes gestures (a new
 * one cancels the previous touch), so true multi-touch needs a kernel-level
 * injection backend (plain-cast's shell core); the second pointer's DOWN is
 * ignored here, matching plain-cast's accessibility fallback.
 */
internal object StreamTouchInjector {
    private const val HOLD_STROKE_MS = 2000L
    private const val HOLD_REFRESH_MS = 1700L
    private const val LIVE_MIN_INTERVAL_MS = 30L
    private const val LIVE_MIN_DUR_MS = 24L
    private const val LIVE_MAX_DUR_MS = 100L
    private const val RELEASE_DUR_MS = 24L
    private const val STALE_INPUT_MS = 10_000L
    // Delay before the system action so the injected touch fully ends first.
    private const val EDGE_ACTION_DELAY_MS = 120L

    private class Stream(val pointerId: Int, x: Float, y: Float, w: Int, h: Int) {
        val downX = x
        val downY = y
        val originX = TouchEdgeGesture.snapToEdge(x, w.toFloat())
        val originY = TouchEdgeGesture.snapToEdge(y, h.toFloat())
        val screenW = w
        val screenH = h
        val edge = TouchEdgeGesture.edgeOf(x, y, w, h)
        var lastX = x
        var lastY = y
        var strokeEndX = x
        var strokeEndY = y
        var live = false
        var stroke: GestureDescription.StrokeDescription? = null
        val buffer = ArrayList<Pair<Float, Float>>(16)
        var bufferStartAt = 0L
        var downAt = 0L
        var strokeDispatchAt = 0L
        var lastLiveAt = 0L
    }

    private var stream: Stream? = null
    private var refreshArmed = false
    private var lastInputAt = 0L

    private val refresh = Runnable { onRefresh() }

    private val strokeCallback = object : AccessibilityService.GestureResultCallback() {
        override fun onCancelled(gestureDescription: GestureDescription?) {
            LogCat.w("TS stroke cancelled at=${SystemClock.uptimeMillis()}")
        }
    }

    private val releaseCallback = object : AccessibilityService.GestureResultCallback() {
        override fun onCompleted(gestureDescription: GestureDescription?) {
            LogCat.d("TS release completed at=${SystemClock.uptimeMillis()}")
        }

        override fun onCancelled(gestureDescription: GestureDescription?) {
            // The lineage expired system-side (hold far past the dangle
            // grace) — the touch was already force-ended there.
            LogCat.w("TS release cancelled (touch already ended by system)")
        }
    }

    fun down(id: Int, x: Float, y: Float, screenWidth: Int = 0, screenHeight: Int = 0) {
        lastInputAt = SystemClock.uptimeMillis()
        val current = stream
        if (current != null) {
            LogCat.d("TS down id=$id ignored (stream active id=${current.pointerId})")
            return
        }
        val path = Path()
        path.moveTo(x, y)
        path.lineTo(x, y)
        val stroke = GestureDescription.StrokeDescription(path, 0, HOLD_STROKE_MS, true)
        if (!dispatch(stroke, strokeCallback)) {
            LogCat.w("TS down dispatch rejected")
            return
        }
        val s = Stream(id, x, y, screenWidth, screenHeight)
        s.downAt = lastInputAt
        s.strokeDispatchAt = lastInputAt
        s.stroke = stroke
        stream = s
        armRefresh(HOLD_REFRESH_MS)
        LogCat.d("TS down id=$id at=$lastInputAt")
    }

    fun move(id: Int, x: Float, y: Float) {
        val s = stream ?: return
        if (s.pointerId != id) return
        lastInputAt = SystemClock.uptimeMillis()
        s.lastX = x
        s.lastY = y
        if (s.buffer.isEmpty()) s.bufferStartAt = lastInputAt
        s.buffer.add(Pair(x, y))
        if (!s.live) {
            if (!TouchEdgeGesture.isBeyondTapSlop(x - s.downX, y - s.downY)) {
                return // stays a single continuous press (tap)
            }
            if (s.edge != TouchEdgeGesture.Edge.NONE) {
                // Edge-originated drags are held (no live strokes — they
                // would scroll the app) and classified at UP: inward swipes
                // fire the matching system action.
                return
            }
        }
        if (lastInputAt - s.lastLiveAt < LIVE_MIN_INTERVAL_MS) return
        dispatchLive(s, lastInputAt)
    }

    private fun dispatchLive(s: Stream, now: Long) {
        if (s.buffer.isEmpty()) return
        val path = Path()
        // First live stroke of a drag re-anchors to the assisted edge
        // origin (equals the down point when not near an edge).
        if (!s.live) {
            path.moveTo(s.originX, s.originY)
        } else {
            path.moveTo(s.strokeEndX, s.strokeEndY)
        }
        for (p in s.buffer) path.lineTo(p.first, p.second)
        val since = if (s.live) now - s.lastLiveAt else now - s.bufferStartAt
        val dur = since.coerceIn(LIVE_MIN_DUR_MS, LIVE_MAX_DUR_MS)
        val stroke = GestureDescription.StrokeDescription(path, 0, dur, true)
        if (!dispatch(stroke, strokeCallback)) return // retry on the next sample
        s.live = true
        s.stroke = stroke
        s.strokeDispatchAt = now
        s.lastLiveAt = now
        s.strokeEndX = s.buffer.last().first
        s.strokeEndY = s.buffer.last().second
        s.buffer.clear()
        armRefresh(HOLD_REFRESH_MS)
    }

    fun up(id: Int, x: Float?, y: Float?) {
        val s = stream ?: run {
            LogCat.w("TS up id=$id but no stream")
            return
        }
        if (s.pointerId != id) return
        lastInputAt = SystemClock.uptimeMillis()
        release(s, x ?: s.lastX, y ?: s.lastY)
    }

    private fun release(s: Stream, endX: Float, endY: Float) {
        val now = SystemClock.uptimeMillis()
        val base = s.stroke
        stream = null
        disarmRefresh()
        if (base == null) return
        var ex = endX
        var ey = endY
        var systemAction = 0
        if (s.edge != TouchEdgeGesture.Edge.NONE && !s.live) {
            val dx = endX - s.downX
            val dy = endY - s.downY
            systemAction = TouchEdgeGesture.actionFor(s.edge, dx, dy, s.screenW, s.screenH)
            if (systemAction != 0) {
                // Qualifying edge swipe: end the touch where it started
                // (no app scroll) and fire the system action after the
                // touch has fully ended.
                ex = s.originX
                ey = s.originY
                s.buffer.clear()
                LogCat.d("TS edge gesture edge=${s.edge} → action=$systemAction")
            }
        }
        val path = Path()
        path.moveTo(s.strokeEndX, s.strokeEndY)
        for (p in s.buffer) path.lineTo(p.first, p.second)
        path.lineTo(ex, ey)
        // Keep the total contact time (press + release segment) under the
        // system long-press threshold for taps.
        val dur = if (!s.live && now - s.downAt > 350L) 1L else RELEASE_DUR_MS
        s.buffer.clear()
        try {
            val stroke = base.continueStroke(path, 0, dur, false)
            val ok = dispatch(stroke, releaseCallback)
            LogCat.d("TS release dur=$dur ok=$ok live=${s.live} at=$now")
        } catch (e: Exception) {
            LogCat.w("TS release failed: ${e.message}")
        }
        if (systemAction != 0) {
            val action = systemAction
            if (TouchInjectThread.isAlive) {
                TouchInjectThread.postDelayed({
                    PlainAccessibilityService.instance?.performGlobalAction(action)
                }, EDGE_ACTION_DELAY_MS)
            } else {
                PlainAccessibilityService.instance?.performGlobalAction(action)
            }
        }
    }

    private fun onRefresh() {
        refreshArmed = false
        val s = stream ?: return
        val now = SystemClock.uptimeMillis()
        if (now - lastInputAt > STALE_INPUT_MS) {
            LogCat.w("TS stale input, force release")
            release(s, s.lastX, s.lastY)
            return
        }
        val sinceStroke = now - s.strokeDispatchAt
        if (sinceStroke < HOLD_REFRESH_MS) {
            armRefresh(HOLD_REFRESH_MS - sinceStroke)
            return
        }
        // Re-arm the dangling stroke before its timeline ends so the touch
        // survives arbitrarily long holds (restarts the touch in place).
        val path = Path()
        path.moveTo(s.strokeEndX, s.strokeEndY)
        path.lineTo(s.strokeEndX, s.strokeEndY)
        val stroke = GestureDescription.StrokeDescription(path, 0, HOLD_STROKE_MS, true)
        if (dispatch(stroke, strokeCallback)) {
            s.stroke = stroke
            s.strokeDispatchAt = now
        }
        armRefresh(HOLD_REFRESH_MS)
    }

    private fun dispatch(
        stroke: GestureDescription.StrokeDescription,
        cb: AccessibilityService.GestureResultCallback,
    ): Boolean {
        val service = PlainAccessibilityService.instance ?: run {
            reset()
            return false
        }
        return service.dispatchGesture(
            GestureDescription.Builder().addStroke(stroke).build(),
            cb,
            TouchInjectThread.handler,
        )
    }

    private fun armRefresh(delayMs: Long) {
        if (refreshArmed) return
        if (TouchInjectThread.postDelayed(refresh, delayMs.coerceAtLeast(1L))) {
            refreshArmed = true
        }
    }

    private fun disarmRefresh() {
        refreshArmed = false
        TouchInjectThread.removeCallbacks(refresh)
    }

    fun reset() {
        stream = null
        disarmRefresh()
    }
}
