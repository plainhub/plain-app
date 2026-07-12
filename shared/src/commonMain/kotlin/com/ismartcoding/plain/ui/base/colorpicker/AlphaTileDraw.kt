package com.ismartcoding.plain.ui.base.colorpicker

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.unit.IntSize

internal fun drawAlphaTileBitmap(
    size: IntSize,
    tileSize: Float,
    tileOddColor: Color,
    tileEvenColor: Color,
): ImageBitmap {
    val bitmap = ImageBitmap(size.width, size.height, ImageBitmapConfig.Argb8888)
    val canvas = Canvas(bitmap)
    val paint = Paint().apply { style = PaintingStyle.Fill }
    val tileCountX = (size.width / tileSize).toInt() + 1
    val tileCountY = (size.height / tileSize).toInt() + 1
    for (y in 0 until tileCountY) {
        for (x in 0 until tileCountX) {
            paint.color = if ((x + y) % 2 == 0) tileOddColor else tileEvenColor
            canvas.drawRect(
                left = x * tileSize, top = y * tileSize,
                right = x * tileSize + tileSize, bottom = y * tileSize + tileSize,
                paint = paint,
            )
        }
    }
    return bitmap
}
