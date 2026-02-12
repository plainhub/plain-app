package com.ismartcoding.plain.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.graphics.Point
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.data.ScreenMirrorControlInput
import com.ismartcoding.plain.enums.ScreenMirrorControlAction

/**
 * Accessibility Service for injecting touch/gesture events during screen mirror remote control.
 *
 * This service uses AccessibilityService.dispatchGesture() to inject:
 * - Tap, long press, swipe (via gesture paths)
 * - Back, Home, Recents (via performGlobalAction)
 *
 * The user must manually enable this service in Android Settings > Accessibility.
 */
class PlainAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        LogCat.d("PlainAccessibilityService connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used — we only need gesture injection
    }

    override fun onInterrupt() {
        // Required override
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        LogCat.d("PlainAccessibilityService destroyed")
    }

    /**
     * Dispatch a screen mirror control event.
     * @param control The control event with normalized coordinates [0, 1]
     * @param screenWidth The actual screen width in pixels
     * @param screenHeight The actual screen height in pixels
     */
    fun dispatchControl(control: ScreenMirrorControlInput, screenWidth: Int, screenHeight: Int) {
        when (control.action) {
            ScreenMirrorControlAction.TAP -> {
                val x = (control.x ?: return) * screenWidth
                val y = (control.y ?: return) * screenHeight
                dispatchTap(x, y)
            }
            ScreenMirrorControlAction.LONG_PRESS -> {
                val x = (control.x ?: return) * screenWidth
                val y = (control.y ?: return) * screenHeight
                val duration = control.duration ?: 500L
                dispatchLongPress(x, y, duration)
            }
            ScreenMirrorControlAction.SWIPE -> {
                val startX = (control.x ?: return) * screenWidth
                val startY = (control.y ?: return) * screenHeight
                val endX = (control.endX ?: return) * screenWidth
                val endY = (control.endY ?: return) * screenHeight
                val duration = control.duration ?: 300L
                dispatchSwipe(startX, startY, endX, endY, duration)
            }
            ScreenMirrorControlAction.SCROLL -> {
                val x = (control.x ?: return) * screenWidth
                val y = (control.y ?: return) * screenHeight
                val deltaY = control.deltaY ?: 0f
                // Convert scroll delta to a swipe: positive deltaY = scroll down = swipe up
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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                }
            }
            ScreenMirrorControlAction.KEY -> {
                // Key injection requires InputManager or root; skip for now
                LogCat.d("Key action not yet supported: ${control.key}")
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

    companion object {
        @Volatile
        var instance: PlainAccessibilityService? = null

        /**
         * Check if the accessibility service is currently enabled.
         */
        fun isEnabled(context: Context = MainApp.instance): Boolean {
            return instance != null
        }

        /**
         * Cached screen size to avoid repeated DisplayMetrics lookups on every control event.
         * Invalidated on configuration changes (rotation, display changes).
         */
        @Volatile
        private var cachedScreenSize: Point? = null

        /**
         * Get the screen size for coordinate mapping (cached).
         */
        fun getScreenSize(context: Context): Point {
            return cachedScreenSize ?: run {
                val displayMetrics = context.resources.displayMetrics
                val size = Point(displayMetrics.widthPixels, displayMetrics.heightPixels)
                cachedScreenSize = size
                size
            }
        }

        /**
         * Invalidate the cached screen size. Call on configuration changes.
         */
        fun invalidateScreenSizeCache() {
            cachedScreenSize = null
        }

        /**
         * Open the system Accessibility Settings page so the user can enable this service.
         */
        fun openAccessibilitySettings(context: Context) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}
