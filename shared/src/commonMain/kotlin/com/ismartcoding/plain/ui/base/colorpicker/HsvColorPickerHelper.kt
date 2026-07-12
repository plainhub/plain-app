package com.ismartcoding.plain.ui.base.colorpicker

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntSize
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal fun createHsvBitmap(
    size: IntSize,
    controller: ColorPickerController,
): ImageBitmap {
    val bitmap = createHsvWheelBitmap(size)

    var dx = 0f
    var dy = 0f
    val scale: Float
    val shaderMatrix = AffineMatrix()
    val mDrawableRect = Rect(0f, 0f, size.width.toFloat(), size.height.toFloat())
    val bitmapWidth: Int = bitmap.width
    val bitmapHeight: Int = bitmap.height

    if (bitmapWidth * mDrawableRect.height > mDrawableRect.width * bitmapHeight) {
        scale = mDrawableRect.height / bitmapHeight.toFloat()
        dx = (mDrawableRect.width - bitmapWidth * scale) * 0.5f
    } else {
        scale = mDrawableRect.width / bitmapWidth.toFloat()
        dy = (mDrawableRect.height - bitmapHeight * scale) * 0.5f
    }
    shaderMatrix.setScale(scale, scale)
    shaderMatrix.postTranslate(
        (dx + 0.5f) + mDrawableRect.left,
        (dy + 0.5f) + mDrawableRect.top,
    )
    controller.imageBitmapMatrix.value = shaderMatrix

    return bitmap
}

internal fun drawInitialColor(
    initialColor: androidx.compose.ui.graphics.Color,
    controller: ColorPickerController,
    centerX: Float,
    centerY: Float,
    onInitialized: () -> Unit,
) {
    val palette = controller.paletteBitmap ?: return
    val pickerRadius: Float = palette.width.coerceAtMost(palette.height) * 0.5f
    if (pickerRadius <= 0) return

    onInitialized()
    val hsv = rgbToHsv(
        (initialColor.red * 255).toInt(),
        (initialColor.green * 255).toInt(),
        (initialColor.blue * 255).toInt(),
    )
    val angle = (PI / 180f) * hsv[0] * (-1)
    val saturationVector = pickerRadius * hsv[1]
    val x = saturationVector * cos(angle) + centerX
    val y = saturationVector * sin(angle) + centerY
    controller.selectByCoordinate(x.toFloat(), y.toFloat(), false)
}
