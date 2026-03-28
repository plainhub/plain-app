package com.ismartcoding.plain.ui.base.colorpicker

import android.graphics.Matrix
import android.graphics.RectF
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.unit.IntSize

internal fun createHsvBitmap(
    size: IntSize,
    context: android.content.Context,
    controller: ColorPickerController,
): Pair<ImageBitmap, HsvBitmapDrawable> {
    val bitmap = ImageBitmap(size.width, size.height, ImageBitmapConfig.Argb8888)
    val drawable = HsvBitmapDrawable(context.resources, bitmap.asAndroidBitmap()).apply {
        setBounds(0, 0, size.width, size.height)
    }

    var dx = 0f
    var dy = 0f
    val scale: Float
    val shaderMatrix = Matrix()
    val mDrawableRect = RectF(0f, 0f, size.width.toFloat(), size.height.toFloat())
    val bitmapWidth: Int = bitmap.asAndroidBitmap().width
    val bitmapHeight: Int = bitmap.asAndroidBitmap().height

    if (bitmapWidth * mDrawableRect.height() > mDrawableRect.width() * bitmapHeight) {
        scale = mDrawableRect.height() / bitmapHeight.toFloat()
        dx = (mDrawableRect.width() - bitmapWidth * scale) * 0.5f
    } else {
        scale = mDrawableRect.width() / bitmapWidth.toFloat()
        dy = (mDrawableRect.height() - bitmapHeight * scale) * 0.5f
    }
    shaderMatrix.setScale(scale, scale)
    shaderMatrix.postTranslate(
        (dx + 0.5f) + mDrawableRect.left,
        (dy + 0.5f) + mDrawableRect.top,
    )
    controller.imageBitmapMatrix.value = shaderMatrix

    return Pair(bitmap, drawable)
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
    val hsv = FloatArray(3)
    android.graphics.Color.RGBToHSV(
        (initialColor.red * 255).toInt(),
        (initialColor.green * 255).toInt(),
        (initialColor.blue * 255).toInt(),
        hsv,
    )
    val angle = (Math.PI / 180f) * hsv[0] * (-1)
    val saturationVector = pickerRadius * hsv[1]
    val x = saturationVector * kotlin.math.cos(angle) + centerX
    val y = saturationVector * kotlin.math.sin(angle) + centerY
    controller.selectByCoordinate(x.toFloat(), y.toFloat(), false)
}
