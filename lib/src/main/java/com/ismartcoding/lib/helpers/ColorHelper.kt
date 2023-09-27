package com.ismartcoding.lib.helpers

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange

object ColorHelper {
    fun isColorLight(
        @ColorInt color: Int,
    ): Boolean {
        val darkness =
            1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
        return darkness < 0.4
    }

    @ColorInt
    fun shiftBackgroundColor(
        @ColorInt backgroundColor: Int,
    ): Int {
        var color = backgroundColor
        color =
            if (isColorLight(color)) {
                shiftColor(color, 0.5f)
            } else {
                shiftColor(color, 1.5f)
            }
        return color
    }

    @ColorInt
    fun shiftColor(
        @ColorInt color: Int,
        @FloatRange(from = 0.0, to = 2.0) by: Float,
    ): Int {
        if (by == 1f) return color
        val alpha = Color.alpha(color)
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[2] *= by // value component
        return (alpha shl 24) + (0x00ffffff and Color.HSVToColor(hsv))
    }
}
