package com.ismartcoding.plain.ui.base.colorpicker

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.unit.IntSize
import kotlin.math.min

internal fun createHsvWheelBitmap(size: IntSize): ImageBitmap {
    val bitmap = ImageBitmap(size.width, size.height, ImageBitmapConfig.Argb8888)
    val canvas = Canvas(bitmap)
    val centerX = size.width / 2f
    val centerY = size.height / 2f
    val radius = min(centerX, centerY)

    // Draw hue wheel using arcs
    val huePaint = Paint().apply { isAntiAlias = true; style = PaintingStyle.Fill }
    for (i in 0..359) {
        huePaint.color = hsvToColor(i.toFloat(), 1f, 1f)
        canvas.drawArc(
            rect = Rect(centerX - radius, centerY - radius, centerX + radius, centerY + radius),
            startAngle = i.toFloat() - 90f,
            sweepAngle = 2f,
            useCenter = true,
            paint = huePaint,
        )
    }

    // Draw saturation overlay (white center -> transparent edge)
    val satPaint = Paint().apply { isAntiAlias = true; style = PaintingStyle.Fill }
    satPaint.shader = RadialGradientShader(
        colors = listOf(Color.White, Color.Transparent),
        center = Offset(centerX, centerY),
        radius = radius,
    )
    canvas.drawCircle(Offset(centerX, centerY), radius, satPaint)

    return bitmap
}
