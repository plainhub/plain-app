package com.ismartcoding.plain.ui.images.editor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.ismartcoding.plain.lib.yjs.CanvasSize
import com.ismartcoding.plain.lib.yjs.RectLayer
import kotlin.math.roundToInt

internal fun constrainCrop(rect: RectLayer, canvas: CanvasSize): RectLayer {
    val x = rect.x.coerceIn(0.0, canvas.width.toDouble())
    val y = rect.y.coerceIn(0.0, canvas.height.toDouble())
    return rect.copy(x = x, y = y, w = rect.w.coerceIn(0.0, canvas.width - x), h = rect.h.coerceIn(0.0, canvas.height - y))
}

internal fun cropEditorBitmap(image: ImageBitmap, crop: RectLayer): ImageBitmap {
    val width = crop.w.roundToInt()
    val height = crop.h.roundToInt()
    require(width >= 2 && height >= 2)
    val result = ImageBitmap(width, height)
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(result), Size(width.toFloat(), height.toFloat())) {
        drawImage(image, srcOffset = IntOffset(crop.x.roundToInt(), crop.y.roundToInt()), srcSize = IntSize(width, height), dstSize = IntSize(width, height))
    }
    return result
}

internal fun DrawScope.drawEditorCrop(crop: RectLayer, scale: Float) {
    val left = crop.x.toFloat(); val top = crop.y.toFloat()
    val width = crop.w.toFloat(); val height = crop.h.toFloat()
    val shade = Color.Black.copy(alpha = .5f)
    drawRect(shade, size = Size(size.width, top))
    drawRect(shade, Offset(0f, top + height), Size(size.width, (size.height - top - height).coerceAtLeast(0f)))
    drawRect(shade, Offset(0f, top), Size(left, height))
    drawRect(shade, Offset(left + width, top), Size((size.width - left - width).coerceAtLeast(0f), height))
    drawRect(Color(0xff3b82f6), Offset(left, top), Size(width, height), style = Stroke(2f / scale))
}
