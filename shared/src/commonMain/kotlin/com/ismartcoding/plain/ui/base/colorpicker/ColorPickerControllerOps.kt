package com.ismartcoding.plain.ui.base.colorpicker

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.sqrt

internal fun ColorPickerController.doSelectByCoordinate(x: Float, y: Float, fromUser: Boolean) {
    val snapPoint = PointMapper.getColorPoint(this, Offset(x, y))
    val extractedColor = if (isHsvColorPalette) {
        extractPixelHsvColor(snapPoint.x, snapPoint.y)
    } else {
        extractPixelColor(snapPoint.x, snapPoint.y)
    }
    if (extractedColor != Color.Transparent) {
        pureSelectedColor.value = extractedColor
        _selectedPoint.value = Offset(snapPoint.x, snapPoint.y)
        _selectedColor.value = applyHSVFactors(extractedColor)
        if (fromUser && debounceDuration != 0L) {
            notifyColorChangedWithDebounce(fromUser)
        } else {
            notifyColorChanged(fromUser)
        }
    }
}

internal fun ColorPickerController.applyHSVFactors(color: Color): Color {
    val hsv = colorToHsv(color)
    if (isAttachedBrightnessSlider) hsv[2] = brightness.value
    return if (isAttachedAlphaSlider) {
        hsvToColor(hsv[0], hsv[1], hsv[2], alpha.value)
    } else {
        hsvToColor(hsv[0], hsv[1], hsv[2])
    }
}

internal fun ColorPickerController.extractPixelColor(x: Float, y: Float): Color {
    val invertMatrix = AffineMatrix()
    imageBitmapMatrix.value.invert(invertMatrix)
    val mappedPoints = floatArrayOf(x, y)
    invertMatrix.mapPoints(mappedPoints)
    val palette = paletteBitmap
    if (palette != null && mappedPoints[0] >= 0 && mappedPoints[1] >= 0 &&
        mappedPoints[0] < palette.width && mappedPoints[1] < palette.height) {
        val pixelMap = palette.toPixelMap()
        val px = mappedPoints[0].toInt().coerceIn(0, palette.width - 1)
        val py = mappedPoints[1].toInt().coerceIn(0, palette.height - 1)
        return pixelMap[px, py]
    }
    return Color.Transparent
}

internal fun ColorPickerController.extractPixelHsvColor(x: Float, y: Float): Color {
    val invertMatrix = AffineMatrix()
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
        (((atan2(y2.toDouble(), -x2.toDouble()) / kotlin.math.PI * 180f).toFloat() + 180)).also { hsv[0] = it }
        hsv[1] = 0f.coerceAtLeast(1f.coerceAtMost((r / radius).toFloat()))
        return hsvToColor(hsv[0], hsv[1], hsv[2])
    }
    return Color.Transparent
}

internal fun ColorPickerController.notifyColorChanged(fromUser: Boolean) {
    val color = _selectedColor.value
    colorChangedTick.value = ColorEnvelope(color, color.hexCode, fromUser)
}

internal fun ColorPickerController.notifyColorChangedWithDebounce(fromUser: Boolean) {
    debounceJob?.cancel()
    debounceJob = debounceScope.launch {
        delay(debounceDuration)
        notifyColorChanged(fromUser)
    }
}
