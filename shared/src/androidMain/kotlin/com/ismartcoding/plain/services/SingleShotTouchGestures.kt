package com.ismartcoding.plain.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import com.ismartcoding.plain.data.TouchPointInput
import com.ismartcoding.plain.lib.logcat.LogCat

/**
 * One-shot gesture dispatch for the legacy JSON control actions
 * (TAP / LONG_PRESS / SWIPE / SCROLL / TOUCH-path): each call is a complete,
 * independent gesture. Streaming control (TOUCH_DOWN/MOVE/UP) lives in
 * [StreamTouchInjector].
 */
internal object SingleShotTouchGestures {
    // Lazy: host unit tests must not trigger Android framework init on class load.
    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

    fun tap(service: AccessibilityService, x: Float, y: Float) {
        val path = Path()
        path.moveTo(x, y)
        val stroke = GestureDescription.StrokeDescription(path, 0, 50)
        service.dispatchGesture(GestureDescription.Builder().addStroke(stroke).build(), null, null)
    }

    fun longPress(service: AccessibilityService, x: Float, y: Float, duration: Long) {
        val path = Path()
        path.moveTo(x, y)
        val stroke = GestureDescription.StrokeDescription(path, 0, duration.coerceAtLeast(500))
        service.dispatchGesture(GestureDescription.Builder().addStroke(stroke).build(), null, null)
    }

    fun swipe(
        service: AccessibilityService,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        duration: Long,
    ) {
        val path = Path()
        path.moveTo(startX, startY)
        path.lineTo(endX, endY)
        val stroke = GestureDescription.StrokeDescription(path, 0, duration.coerceAtLeast(50))
        service.dispatchGesture(GestureDescription.Builder().addStroke(stroke).build(), null, null)
    }

    /** Replay a recorded touch path (web-client TOUCH action with pathPoints). */
    fun touchPath(
        service: AccessibilityService,
        points: List<TouchPointInput>,
        screenWidth: Int,
        screenHeight: Int,
    ) {
        if (points.size == 1) {
            val p = points.first()
            val x = normToPx(p.x, screenWidth)
            val y = normToPx(p.y, screenHeight)
            val duration = p.tMs.coerceAtLeast(0).toLong()
            if (duration >= 500L) {
                longPress(service, x, y, duration)
            } else {
                tap(service, x, y)
            }
            return
        }

        val sorted = points.sortedBy { it.tMs }
        val first = sorted.first()
        val last = sorted.last()
        val totalDuration = (last.tMs - first.tMs).coerceAtLeast(16).toLong()

        val fx = normToPx(first.x, screenWidth)
        val fy = normToPx(first.y, screenHeight)
        val lx = normToPx(last.x, screenWidth)
        val ly = normToPx(last.y, screenHeight)
        val dx = lx - fx
        val dy = ly - fy
        val totalDistance = kotlin.math.sqrt(dx * dx + dy * dy)

        if (totalDistance < 4f) {
            if (totalDuration >= 500L) {
                longPress(service, fx, fy, totalDuration)
            } else {
                tap(service, fx, fy)
            }
            return
        }

        val path = Path()
        path.moveTo(fx, fy)
        for (i in 1 until sorted.size) {
            val pt = sorted[i]
            path.lineTo(normToPx(pt.x, screenWidth), normToPx(pt.y, screenHeight))
        }
        val d = totalDuration.coerceAtLeast(16)
        val stroke = GestureDescription.StrokeDescription(path, 0, d, false)
        LogCat.d("TouchPath: ${sorted.size} pts, ${d}ms")
        service.dispatchGesture(
            GestureDescription.Builder().addStroke(stroke).build(),
            null,
            mainHandler,
        )
    }

    private fun normToPx(norm: Float, dim: Int): Float =
        norm.coerceIn(0f, 1f - 1e-4f) * dim
}
