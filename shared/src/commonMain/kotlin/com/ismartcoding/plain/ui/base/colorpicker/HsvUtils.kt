package com.ismartcoding.plain.ui.base.colorpicker

import androidx.compose.ui.graphics.Color

/** Convert HSV [0..360], [0..1], [0..1] to ARGB Color. */
internal fun hsvToColor(h: Float, s: Float, v: Float, alpha: Float = 1f): Color {
    val c = v * s
    val x = c * (1 - kotlin.math.abs((h / 60f) % 2 - 1))
    val m = v - c
    val (r, g, b) = when {
        h < 60f -> Triple(c, x, 0f)
        h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x)
        h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(r + m, g + m, b + m, alpha)
}

/** Convert Color to HSV [0..360], [0..1], [0..1]. */
internal fun colorToHsv(color: Color): FloatArray {
    val r = color.red
    val g = color.green
    val b = color.blue
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val d = max - min
    val h = when {
        d == 0f -> 0f
        max == r -> 60f * (((g - b) / d) % 6)
        max == g -> 60f * ((b - r) / d + 2)
        else -> 60f * ((r - g) / d + 4)
    }
    val s = if (max == 0f) 0f else d / max
    return floatArrayOf(if (h < 0) h + 360f else h, s, max)
}

/** Convert RGB [0..255] to HSV. */
internal fun rgbToHsv(r: Int, g: Int, b: Int): FloatArray =
    colorToHsv(Color(r / 255f, g / 255f, b / 255f))
