package com.ismartcoding.plain.services
import com.ismartcoding.plain.appContext

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.graphics.Point
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.data.ScreenMirrorControlInput
import com.ismartcoding.plain.data.TouchPointInput
import com.ismartcoding.plain.enums.ScreenMirrorControlAction
import com.ismartcoding.plain.services.screenmirror.getRealScreenSize as getMirrorRealScreenSize

class PlainAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        LogCat.d("PlainAccessibilityService connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    }

    override fun onInterrupt() {
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        LogCat.d("PlainAccessibilityService destroyed")
    }

    private fun clampNorm(v: Float): Float = v.coerceIn(0f, 1f - 1e-4f)

    private fun normToX(norm: Float, screenWidth: Int): Float =
        clampNorm(norm) * screenWidth

    private fun normToY(norm: Float, screenHeight: Int): Float =
        clampNorm(norm) * screenHeight

    fun dispatchControl(control: ScreenMirrorControlInput, screenWidth: Int, screenHeight: Int) {
        when (control.action) {
            ScreenMirrorControlAction.TAP -> {
                val x = normToX(control.x ?: return, screenWidth)
                val y = normToY(control.y ?: return, screenHeight)
                dispatchTap(x, y)
            }

            ScreenMirrorControlAction.LONG_PRESS -> {
                val x = normToX(control.x ?: return, screenWidth)
                val y = normToY(control.y ?: return, screenHeight)
                val duration = control.duration ?: 500L
                dispatchLongPress(x, y, duration)
            }

            ScreenMirrorControlAction.SWIPE -> {
                val startX = normToX(control.x ?: return, screenWidth)
                val startY = normToY(control.y ?: return, screenHeight)
                val endX = normToX(control.endX ?: return, screenWidth)
                val endY = normToY(control.endY ?: return, screenHeight)
                val duration = control.duration ?: 300L
                dispatchSwipe(startX, startY, endX, endY, duration)
            }

            ScreenMirrorControlAction.SCROLL -> {
                val x = normToX(control.x ?: return, screenWidth)
                val y = normToY(control.y ?: return, screenHeight)
                val deltaY = control.deltaY ?: 0f
                val scrollDistance = deltaY.coerceIn(-500f, 500f)
                dispatchSwipe(x, y, x, y + scrollDistance, 200L)
            }

            ScreenMirrorControlAction.BACK -> {
                performGlobalAction(GLOBAL_ACTION_BACK)
            }

            ScreenMirrorControlAction.HOME -> {
                performGlobalAction(GLOBAL_ACTION_HOME)
            }

            ScreenMirrorControlAction.RECENTS -> {
                performGlobalAction(GLOBAL_ACTION_RECENTS)
            }

            ScreenMirrorControlAction.LOCK_SCREEN -> {
                performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
            }

            ScreenMirrorControlAction.KEY -> {
                LogCat.d("Key action not yet supported: ${control.key}")
            }

            ScreenMirrorControlAction.TOUCH -> {
                val points = control.pathPoints
                if (points == null || points.isEmpty()) return
                dispatchTouchPath(points, screenWidth, screenHeight)
            }

            ScreenMirrorControlAction.TOUCH_DOWN -> {
                val x = normToX(control.x ?: return, screenWidth)
                val y = normToY(control.y ?: return, screenHeight)
                TouchGestureStream.begin(this, x, y)
            }

            ScreenMirrorControlAction.TOUCH_MOVE -> {
                val x = normToX(control.x ?: return, screenWidth)
                val y = normToY(control.y ?: return, screenHeight)
                TouchGestureStream.move(this, x, y)
            }

            ScreenMirrorControlAction.TOUCH_UP -> {
                val x = if (control.x != null) normToX(control.x, screenWidth) else null
                val y = if (control.y != null) normToY(control.y, screenHeight) else null
                TouchGestureStream.end(this, x, y)
            }
        }
    }

    private fun dispatchTap(x: Float, y: Float) {
        val path = Path()
        path.moveTo(x, y)
        val stroke = GestureDescription.StrokeDescription(path, 0, 50)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, null, null)
    }

    private fun dispatchLongPress(x: Float, y: Float, duration: Long) {
        val path = Path()
        path.moveTo(x, y)
        val stroke = GestureDescription.StrokeDescription(path, 0, duration.coerceAtLeast(500))
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, null, null)
    }

    private fun dispatchSwipe(startX: Float, startY: Float, endX: Float, endY: Float, duration: Long) {
        val path = Path()
        path.moveTo(startX, startY)
        path.lineTo(endX, endY)
        val stroke = GestureDescription.StrokeDescription(path, 0, duration.coerceAtLeast(50))
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, null, null)
    }

    private fun dispatchTouchPath(
        points: List<TouchPointInput>,
        screenWidth: Int,
        screenHeight: Int,
    ) {
        if (points.size == 1) {
            val p = points.first()
            val x = normToX(p.x, screenWidth)
            val y = normToY(p.y, screenHeight)
            val duration = p.t.coerceAtLeast(0).toLong()
            if (duration >= 500L) {
                dispatchLongPress(x, y, duration)
            } else {
                dispatchTap(x, y)
            }
            return
        }

        val sorted = points.sortedBy { it.t }
        val first = sorted.first()
        val last = sorted.last()
        val totalDuration = (last.t - first.t).coerceAtLeast(16).toLong()

        val fx = normToX(first.x, screenWidth)
        val fy = normToY(first.y, screenHeight)
        val lx = normToX(last.x, screenWidth)
        val ly = normToY(last.y, screenHeight)
        val dx = lx - fx
        val dy = ly - fy
        val totalDistance = kotlin.math.sqrt(dx * dx + dy * dy)

        if (totalDistance < 4f) {
            if (totalDuration >= 500L) {
                dispatchLongPress(fx, fy, totalDuration)
            } else {
                dispatchTap(fx, fy)
            }
            return
        }

        val path = Path()
        path.moveTo(fx, fy)
        for (i in 1 until sorted.size) {
            val pt = sorted[i]
            path.lineTo(normToX(pt.x, screenWidth), normToY(pt.y, screenHeight))
        }
        dispatchPathGesture(path, totalDuration, false, null)
    }

    private fun dispatchPathGesture(
        path: Path,
        duration: Long,
        willContinue: Boolean,
        callback: GestureResultCallback?,
    ) {
        val d = duration.coerceAtLeast(16)
        val stroke = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            GestureDescription.StrokeDescription(path, 0, d, willContinue)
        } else {
            @Suppress("DEPRECATION")
            GestureDescription.StrokeDescription(path, 0, d)
        }
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, callback, mainHandler)
    }

    private object TouchGestureStream {
        private const val SEGMENT_DURATION_MS = 20L
        private const val FINAL_DURATION_MS = 24L

        private val lock = Any()
        private var active = false
        private var lastX = 0f
        private var lastY = 0f
        private var inFlight = false
        private var pending: Pair<Float, Float>? = null

        private val callback = object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                val next = synchronized(lock) {
                    val p = pending
                    pending = null
                    if (p == null) inFlight = false
                    p
                }
                val svc = instance
                if (next != null && svc != null && active) {
                    dispatchMoveLocked(svc, next.first, next.second)
                }
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                LogCat.w("TouchGestureStream: gesture cancelled, resetting stream")
                synchronized(lock) {
                    active = false
                    inFlight = false
                    pending = null
                }
            }
        }

        fun begin(service: PlainAccessibilityService, x: Float, y: Float) {
            synchronized(lock) {
                active = true
                inFlight = true
                pending = null
                lastX = x
                lastY = y
            }
            val path = Path()
            path.moveTo(x, y)
            path.lineTo(x, y)
            service.dispatchPathGesture(path, SEGMENT_DURATION_MS, true, callback)
        }

        fun move(service: PlainAccessibilityService, x: Float, y: Float) {
            val shouldDispatch = synchronized(lock) {
                if (!active) {
                    active = true
                    inFlight = true
                    pending = null
                    lastX = x
                    lastY = y
                    0
                } else if (inFlight) {
                    pending = Pair(x, y)
                    -1
                } else {
                    inFlight = true
                    pending = null
                    1
                }
            }
            when (shouldDispatch) {
                0 -> {
                    val path = Path()
                    path.moveTo(x, y)
                    path.lineTo(x, y)
                    service.dispatchPathGesture(path, SEGMENT_DURATION_MS, true, callback)
                }
                1 -> dispatchMoveLocked(service, x, y)
            }
        }

        private fun dispatchMoveLocked(service: PlainAccessibilityService, x: Float, y: Float) {
            val fromX: Float
            val fromY: Float
            synchronized(lock) {
                fromX = lastX
                fromY = lastY
                lastX = x
                lastY = y
            }
            val path = Path()
            path.moveTo(fromX, fromY)
            path.lineTo(x, y)
            service.dispatchPathGesture(path, SEGMENT_DURATION_MS, true, callback)
        }

        fun end(service: PlainAccessibilityService, x: Float?, y: Float?) {
            val (fromX, fromY, endX, endY) = synchronized(lock) {
                if (!active) return
                val ex = x ?: lastX
                val ey = y ?: lastY
                val fx = lastX
                val fy = lastY
                active = false
                inFlight = false
                pending = null
                listOf(fx, fy, ex, ey)
            }
            val path = Path()
            path.moveTo(fromX, fromY)
            path.lineTo(endX, endY)
            service.dispatchPathGesture(path, FINAL_DURATION_MS, false, null)
        }
    }

    companion object {
        private val mainHandler = Handler(Looper.getMainLooper())

        @Volatile
        var instance: PlainAccessibilityService? = null

        fun isEnabled(context: Context = appContext): Boolean {
            return instance != null
        }

        @Volatile
        private var cachedScreenSize: Point? = null

        fun getScreenSize(context: Context): Point {
            return cachedScreenSize ?: run {
                val size = getRealScreenSize(context)
                cachedScreenSize = size
                size
            }
        }

        private fun getRealScreenSize(context: Context): Point {
            return getMirrorRealScreenSize(context)
        }

        fun invalidateScreenSizeCache() {
            cachedScreenSize = null
        }

        fun openAccessibilitySettings(context: Context) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}
