package com.ismartcoding.plain.services.webrtc

import android.content.Context
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.util.DisplayMetrics
import android.view.Display
import android.view.Surface
import android.view.WindowManager
import com.ismartcoding.lib.isSPlus
import com.ismartcoding.plain.data.DScreenMirrorQuality
import com.ismartcoding.plain.enums.ScreenMirrorMode
import kotlin.math.max
import kotlin.math.min

internal fun getEffectiveResolution(quality: DScreenMirrorQuality, adaptiveResolution: Int): Int {
    return when (quality.mode) {
        ScreenMirrorMode.AUTO -> adaptiveResolution
        ScreenMirrorMode.HD -> 1080
        ScreenMirrorMode.SMOOTH -> 720
    }
}

internal fun computeTargetBitrateKbps(resolution: Int): Int {
    return when {
        resolution >= 1080 -> 4000
        resolution >= 720 -> 2000
        else -> 1000
    }
}

internal fun computeStartBitrateKbps(resolution: Int): Int {
    return when {
        resolution >= 1080 -> 3000
        resolution >= 720 -> 1500
        else -> 800
    }
}

internal fun computeCaptureSize(
    context: Context,
    quality: DScreenMirrorQuality,
    adaptiveResolution: Int,
): Triple<Int, Int, Int> {
    val realSize = getRealScreenSize(context)
    val shortSide = min(realSize.x, realSize.y)
    val targetShort = getEffectiveResolution(quality, adaptiveResolution)
    val scale = min(1f, targetShort.toFloat() / shortSide.toFloat())
    val targetWidth = makeEven(max(2, (realSize.x * scale).toInt()))
    val targetHeight = makeEven(max(2, (realSize.y * scale).toInt()))
    return Triple(targetWidth, targetHeight, context.resources.displayMetrics.densityDpi)
}

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

internal fun makeEven(value: Int): Int = if (value % 2 == 0) value else value - 1
