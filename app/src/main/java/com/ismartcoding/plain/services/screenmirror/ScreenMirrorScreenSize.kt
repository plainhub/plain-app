package com.ismartcoding.plain.services.screenmirror

import android.content.Context
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.util.DisplayMetrics
import android.view.Display
import android.view.Surface
import android.view.WindowManager
import com.ismartcoding.plain.lib.isSPlus

/**
 * Get the real physical screen dimensions including system bars.
 *
 * Primary: DisplayManager.getDisplay(DEFAULT_DISPLAY).getRealMetrics() — reliable
 * from a Service context and correctly reflects the active display on foldable devices.
 *
 * Fallback: WindowManager-based logic for devices where DisplayManager reports 0×0.
 */
internal fun getRealScreenSize(context: Context): Point {
    val dm = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    val display = dm.getDisplay(Display.DEFAULT_DISPLAY)
    if (display != null) {
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        display.getRealMetrics(metrics)
        if (metrics.widthPixels > 0 && metrics.heightPixels > 0) {
            return Point(metrics.widthPixels, metrics.heightPixels)
        }
    }
    val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    return if (isSPlus()) {
        val bounds = wm.currentWindowMetrics.bounds
        Point(bounds.width(), bounds.height())
    } else {
        @Suppress("DEPRECATION")
        val d = wm.defaultDisplay
        val mode = d.mode
        var w = mode.physicalWidth
        var h = mode.physicalHeight
        if (w > 0 && h > 0) {
            @Suppress("DEPRECATION")
            val rotation = d.rotation
            if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) {
                val tmp = w; w = h; h = tmp
            }
            Point(w, h)
        } else {
            val size = Point()
            @Suppress("DEPRECATION")
            d.getRealSize(size)
            size
        }
    }
}
