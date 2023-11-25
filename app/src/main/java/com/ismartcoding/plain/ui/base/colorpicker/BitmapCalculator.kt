package com.ismartcoding.plain.ui.base.colorpicker

import android.graphics.Bitmap
import android.media.ThumbnailUtils
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
  internal fun scaleBitmap(bitmap: Bitmap, targetSize: IntSize): Bitmap {
    return Bitmap.createScaledBitmap(
      bitmap,
      targetSize.width,
      targetSize.height,
      false,
    )
  }

  /**
   * Crop ths source the corresponding dimension of the target size.
   * so that if the dimensions (width and height) source is bigger than the target size,
   * it will be cut off from the center.
   */
  internal fun cropBitmap(bitmap: Bitmap, targetSize: IntSize): Bitmap {
    return ThumbnailUtils.extractThumbnail(bitmap, targetSize.width, targetSize.height)
  }

  /**
   * Scale the source with maintaining the source's aspect ratio
   * so that if both dimensions (width and height) of the source is smaller than the target size,
   * it will not be scaled.
   */
  internal fun inside(bitmap: Bitmap, targetSize: IntSize): Bitmap {
    return if (bitmap.width < targetSize.width && bitmap.height < targetSize.height) {
      bitmap
    } else {
      scaleBitmap(bitmap, targetSize)
    }
  }
}
