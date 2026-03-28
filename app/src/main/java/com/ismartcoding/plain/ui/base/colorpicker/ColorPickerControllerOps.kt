package com.ismartcoding.plain.ui.base.colorpicker

import android.graphics.Matrix
import android.graphics.PointF
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import kotlin.math.atan2
import kotlin.math.sqrt

internal fun ColorPickerController.doSelectByCoordinate(x: Float, y: Float, fromUser: Boolean) {
    val snapPoint = PointMapper.getColorPoint(this, PointF(x, y))
    val extractedColor = if (isHsvColorPalette) {
        extractPixelHsvColor(snapPoint.x, snapPoint.y)
    } else {
        extractPixelColor(snapPoint.x, snapPoint.y)
    }
    if (extractedColor != Color.Transparent) {
        pureSelectedColor.value = extractedColor
        _selectedPoint.value = PointF(snapPoint.x, snapPoint.y)
        _selectedColor.value = applyHSVFactors(extractedColor)
        if (fromUser && debounceDuration != 0L) {
            notifyColorChangedWithDebounce(fromUser)
        } else {
            notifyColorChanged(fromUser)
        }
    }
}

internal fun ColorPickerController.applyHSVFactors(color: Color): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
    if (isAttachedBrightnessSlider) hsv[2] = brightness.value
    return if (isAttachedAlphaSlider) {
        Color(android.graphics.Color.HSVToColor((alpha.value * 255).toInt(), hsv))
    } else {
        Color(android.graphics.Color.HSVToColor(hsv))
    }
}

internal fun ColorPickerController.extractPixelColor(x: Float, y: Float): Color {
    val invertMatrix = Matrix()
    imageBitmapMatrix.value.invert(invertMatrix)
    val mappedPoints = floatArrayOf(x, y)
    invertMatrix.mapPoints(mappedPoints)
    val palette = paletteBitmap
    if (palette != null && mappedPoints[0] >= 0 && mappedPoints[1] >= 0 &&
        mappedPoints[0] < palette.width && mappedPoints[1] < palette.height) {
        val scaleX = mappedPoints[0] / palette.width
        val x1 = scaleX * palette.width
        val scaleY = mappedPoints[1] / palette.height
        val y1 = scaleY * palette.height
        val pixelColor = palette.asAndroidBitmap().getPixel(x1.toInt(), y1.toInt())
        return Color(pixelColor)
    }
    return Color.Transparent
}

internal fun ColorPickerController.extractPixelHsvColor(x: Float, y: Float): Color {
    val invertMatrix = Matrix()
    imageBitmapMatrix.value.invert(invertMatrix)
    val mappedPoints = floatArrayOf(x, y)
    invertMatrix.mapPoints(mappedPoints)
    val palette = paletteBitmap
    if (palette != null && mappedPoints[0] >= 0 && mappedPoints[1] >= 0 &&
        mappedPoints[0] < palette.width && mappedPoints[1] < palette.height) {
        val x2 = x - palette.width * 0.5f
        val y2 = y - palette.height * 0.5f
        val size = canvasSize.value
        val r = sqrt((x2 * x2 + y2 * y2).toDouble())
        val radius: Float = size.width.coerceAtMost(size.height) * 0.5f
        val hsv = floatArrayOf(0f, 0f, 1f)
        (((atan2(y2.toDouble(), -x2.toDouble()) / Math.PI * 180f).toFloat() + 180)).also { hsv[0] = it }
        hsv[1] = 0f.coerceAtLeast(1f.coerceAtMost((r / radius).toFloat()))
        return Color(android.graphics.Color.HSVToColor(hsv))
    }
    return Color.Transparent
}

internal fun ColorPickerController.notifyColorChanged(fromUser: Boolean) {
    val color = _selectedColor.value
    colorChangedTick.value = ColorEnvelope(color, color.hexCode, fromUser)
}

internal fun ColorPickerController.notifyColorChangedWithDebounce(fromUser: Boolean) {
    val runnable = { notifyColorChanged(fromUser) }
    debounceHandler.removeCallbacksAndMessages(null)
    debounceHandler.postDelayed(runnable, debounceDuration)
}
