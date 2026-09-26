package com.ismartcoding.plain.services
import com.ismartcoding.plain.appContext

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.graphics.Point
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.data.ScreenMirrorControlInput
import com.ismartcoding.plain.enums.ScreenMirrorControlAction
import com.ismartcoding.plain.services.screenmirror.getRealScreenSize as getMirrorRealScreenSize

/**
 * The accessibility service: publishes its singleton instance, routes remote
 * control inputs to the right injector and exposes screen-size helpers.
 *
 * Injection responsibilities live in their own units:
 *  - [StreamTouchInjector] — streaming TOUCH_DOWN/MOVE/UP (dangling stroke +
 *    terminal release, edge gestures)
 *  - [SingleShotTouchGestures] — legacy one-shot actions (TAP/SWIPE/…)
 *  - [TouchInjectThread] — the shared injection thread
 */
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
        StreamTouchInjector.reset()
        TouchInjectThread.shutdown()
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
                SingleShotTouchGestures.tap(this, x, y)
            }

            ScreenMirrorControlAction.LONG_PRESS -> {
                val x = normToX(control.x ?: return, screenWidth)
                val y = normToY(control.y ?: return, screenHeight)
                val durationMs = control.durationMs ?: 500L
                SingleShotTouchGestures.longPress(this, x, y, durationMs)
            }

            ScreenMirrorControlAction.SWIPE -> {
                val startX = normToX(control.x ?: return, screenWidth)
                val startY = normToY(control.y ?: return, screenHeight)
                val endX = normToX(control.endX ?: return, screenWidth)
                val endY = normToY(control.endY ?: return, screenHeight)
                val durationMs = control.durationMs ?: 300L
                SingleShotTouchGestures.swipe(this, startX, startY, endX, endY, durationMs)
            }

            ScreenMirrorControlAction.SCROLL -> {
                val x = normToX(control.x ?: return, screenWidth)
                val y = normToY(control.y ?: return, screenHeight)
                val dx = (control.deltaX ?: 0f).coerceIn(-500f, 500f)
                val dy = (control.deltaY ?: 0f).coerceIn(-500f, 500f)
                if (dx == 0f && dy == 0f) return
                SingleShotTouchGestures.swipe(this, x, y, x + dx, y + dy, 200L)
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
                SingleShotTouchGestures.touchPath(this, points, screenWidth, screenHeight)
            }

            ScreenMirrorControlAction.TOUCH_DOWN -> {
                val x = normToX(control.x ?: return, screenWidth)
                val y = normToY(control.y ?: return, screenHeight)
                val pointerId = control.pointerId ?: 0
                TouchInjectThread.handler.post {
                    StreamTouchInjector.down(pointerId, x, y, screenWidth, screenHeight)
                }
            }

            ScreenMirrorControlAction.TOUCH_MOVE -> {
                val x = normToX(control.x ?: return, screenWidth)
                val y = normToY(control.y ?: return, screenHeight)
                val pointerId = control.pointerId ?: 0
                TouchInjectThread.handler.post { StreamTouchInjector.move(pointerId, x, y) }
            }

            ScreenMirrorControlAction.TOUCH_UP -> {
                val x = if (control.x != null) normToX(control.x, screenWidth) else null
                val y = if (control.y != null) normToY(control.y, screenHeight) else null
                val pointerId = control.pointerId ?: 0
                TouchInjectThread.handler.post { StreamTouchInjector.up(pointerId, x, y) }
            }
        }
    }

    companion object {
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
