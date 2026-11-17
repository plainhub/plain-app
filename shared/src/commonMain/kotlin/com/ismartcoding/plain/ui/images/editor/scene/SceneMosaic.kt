package com.ismartcoding.plain.ui.images.editor.scene

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.ismartcoding.plain.lib.yjs.MosaicLayer
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

internal fun DrawScope.drawSceneMosaic(bitmap: ImageBitmap, layer: MosaicLayer) {
    val left = floor(layer.x).toInt().coerceAtLeast(0)
    val top = floor(layer.y).toInt().coerceAtLeast(0)
    val width = ceil(layer.w).toInt().coerceAtMost(bitmap.width - left)
    val height = ceil(layer.h).toInt().coerceAtMost(bitmap.height - top)
    if (width <= 0 || height <= 0) return
    val pixels = IntArray(width * height)
    bitmap.readPixels(pixels, startX = left, startY = top, width = width, height = height)
    val block = layer.blockSize.roundToInt().coerceAtLeast(1)
    for (y in 0 until height step block) for (x in 0 until width step block) {
        val blockWidth = minOf(block, width - x)
        val blockHeight = minOf(block, height - y)
        var red = 0L; var green = 0L; var blue = 0L
        for (row in y until y + blockHeight) for (column in x until x + blockWidth) {
            val pixel = pixels[row * width + column]
            red += (pixel ushr 16) and 255
            green += (pixel ushr 8) and 255
            blue += pixel and 255
        }
        val count = blockWidth * blockHeight
        val color = Color((red.toDouble() / count).roundToInt(), (green.toDouble() / count).roundToInt(), (blue.toDouble() / count).roundToInt())
        drawRect(color, Offset((left + x).toFloat(), (top + y).toFloat()), Size(blockWidth.toFloat(), blockHeight.toFloat()), blendMode = BlendMode.SrcAtop)
    }
}
