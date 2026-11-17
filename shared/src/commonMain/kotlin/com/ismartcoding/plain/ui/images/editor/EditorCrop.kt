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
    return rect.copy(x = x, y = y, w = ((rect.x + rect.w).coerceIn(0.0, canvas.width.toDouble()) - x).coerceAtLeast(0.0), h = ((rect.y + rect.h).coerceIn(0.0, canvas.height.toDouble()) - y).coerceAtLeast(0.0))
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

internal fun DrawScope.drawEditorCrop(crop: RectLayer, canvas: CanvasSize, scale: Float) {
    val canvasWidth = canvas.width.toFloat(); val canvasHeight = canvas.height.toFloat()
    val left = crop.x.toFloat(); val top = crop.y.toFloat()
    val width = crop.w.toFloat(); val height = crop.h.toFloat()
    val shade = Color.Black.copy(alpha = .5f)
    drawRect(shade, size = Size(canvasWidth, top))
    drawRect(shade, Offset(0f, top + height), Size(canvasWidth, (canvasHeight - top - height).coerceAtLeast(0f)))
    drawRect(shade, Offset(0f, top), Size(left, height))
    drawRect(shade, Offset(left + width, top), Size((canvasWidth - left - width).coerceAtLeast(0f), height))
    drawRect(Color(0xff3b82f6), Offset(left, top), Size(width, height), style = Stroke(2f / scale))
    val side = 12f * density / scale
    for (corner in listOf(Offset(left, top), Offset(left + width, top), Offset(left, top + height), Offset(left + width, top + height))) {
        drawRect(Color.White, corner - Offset(side / 2, side / 2), Size(side, side))
        drawRect(Color(0xff3b82f6), corner - Offset(side / 2, side / 2), Size(side, side), style = Stroke(2f * density / scale))
    }
}

internal fun moveEditorCrop(crop: RectLayer, dx: Double, dy: Double, canvas: CanvasSize): RectLayer = crop.copy(
    x = (crop.x + dx).coerceIn(0.0, canvas.width - crop.w),
    y = (crop.y + dy).coerceIn(0.0, canvas.height - crop.h),
)

internal fun resizeEditorCrop(crop: RectLayer, handle: ResizeHandle, point: com.ismartcoding.plain.lib.yjs.Point, canvas: CanvasSize): RectLayer {
    val oppositeX = if (handle == ResizeHandle.TOP_LEFT || handle == ResizeHandle.BOTTOM_LEFT) crop.x + crop.w else crop.x
    val oppositeY = if (handle == ResizeHandle.TOP_LEFT || handle == ResizeHandle.TOP_RIGHT) crop.y + crop.h else crop.y
    val x = point.x.coerceIn(0.0, canvas.width.toDouble())
    val y = point.y.coerceIn(0.0, canvas.height.toDouble())
    return crop.copy(x = minOf(x, oppositeX), y = minOf(y, oppositeY), w = kotlin.math.abs(x - oppositeX), h = kotlin.math.abs(y - oppositeY))
}
