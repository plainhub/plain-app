package com.ismartcoding.plain.ui.base.colorpicker

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/**
 * A bitmap calculator to scaling and cropping to a target size.
 */
internal object BitmapCalculator {

  /**
   * Scale the source with maintaining the source's aspect ratio
   * so that both dimensions (width and height) of the source will be equal to or less than the
   * corresponding dimension of the target size.
   */
  internal fun scaleBitmap(bitmap: ImageBitmap, targetSize: IntSize): ImageBitmap {
    val result = ImageBitmap(targetSize.width, targetSize.height, ImageBitmapConfig.Argb8888)
    val canvas = Canvas(result)
    canvas.drawImageRect(
      image = bitmap,
      srcOffset = IntOffset(0, 0),
      srcSize = IntSize(bitmap.width, bitmap.height),
      dstOffset = IntOffset(0, 0),
      dstSize = targetSize,
      paint = Paint(),
    )
    return result
  }

  /**
   * Crop ths source the corresponding dimension of the target size.
   * so that if the dimensions (width and height) source is bigger than the target size,
   * it will be cut off from the center.
   */
  internal fun cropBitmap(bitmap: ImageBitmap, targetSize: IntSize): ImageBitmap {
    val result = ImageBitmap(targetSize.width, targetSize.height, ImageBitmapConfig.Argb8888)
    val canvas = Canvas(result)
    val srcW = bitmap.width
    val srcH = bitmap.height
    val targetRatio = targetSize.width.toFloat() / targetSize.height.toFloat()
    val srcRatio = srcW.toFloat() / srcH.toFloat()
    val srcOffset: IntOffset
    val srcSize: IntSize
    if (srcRatio > targetRatio) {
      val newW = (srcH * targetRatio).toInt()
      srcOffset = IntOffset((srcW - newW) / 2, 0)
      srcSize = IntSize(newW, srcH)
    } else {
      val newH = (srcW / targetRatio).toInt()
      srcOffset = IntOffset(0, (srcH - newH) / 2)
      srcSize = IntSize(srcW, newH)
    }
    canvas.drawImageRect(
      image = bitmap,
      srcOffset = srcOffset,
      srcSize = srcSize,
      dstOffset = IntOffset(0, 0),
      dstSize = targetSize,
      paint = Paint(),
    )
    return result
  }

  /**
   * Scale the source with maintaining the source's aspect ratio
   * so that if both dimensions (width and height) of the source is smaller than the target size,
   * it will not be scaled.
   */
  internal fun inside(bitmap: ImageBitmap, targetSize: IntSize): ImageBitmap {
    return if (bitmap.width < targetSize.width && bitmap.height < targetSize.height) {
      bitmap
    } else {
      scaleBitmap(bitmap, targetSize)
    }
  }
}
