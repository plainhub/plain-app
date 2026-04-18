package com.ismartcoding.plain.thumbnail

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface

/**
 * Fast single-pass image decode using the Glide-style density trick.
 *
 * Strategy:
 *  1. inJustDecodeBounds — read source dimensions, no pixel allocation.
 *  2. Read EXIF orientation once (all 8 variants: rotate + flip combinations).
 *  3. inSampleSize — coarse power-of-2 subsample. For 90°/270° rotated images
 *     the requested width/height are swapped before computing the sample size,
 *     because raw pixels are stored with transposed axes.
 *  4. inDensity / inTargetDensity — precise fractional scale applied *inside*
 *     the native decoder (libjpeg-turbo folds this into its IDCT stage for
 *     JPEG), eliminating a second Bitmap allocation from createScaledBitmap().
 *  5. Apply EXIF transform matrix (rotate + optional flip) to the decoded bitmap
 *     BEFORE the center-crop step so the crop operates on the correct orientation.
 *  6. Conditional sharpening — only for small outputs (≤ SHARPEN_THRESHOLD px)
 *     where the JVM pixel loop is cheap (< 10 ms). Large outputs (e.g. 1024 px
 *     lightbox) skip sharpening to avoid the ~150 ms overhead.
 *
 * EXIF note:
 *  BitmapFactory.decodeFile() never auto-rotates by EXIF, regardless of Android
 *  version. Only ImageDecoder (API 28+) and ContentResolver.loadThumbnail()
 *  handle EXIF automatically. For the BitmapFactory path we must apply it manually.
 *
 * Returns null if BitmapFactory cannot decode the file (unsupported format).
 * Callers should fall back to Coil for formats like embedded audio art.
 */
internal const val SHARPEN_THRESHOLD = 300

fun decodeSampledBitmapFromFile(
    path: String,
    reqWidth: Int,
    reqHeight: Int,
    centerCrop: Boolean,
): Bitmap? {
    // Pass 1 — read dimensions only (no pixel allocation)
    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, opts)
    val srcW = opts.outWidth
    val srcH = opts.outHeight
    if (srcW <= 0 || srcH <= 0) return null

    // Read EXIF orientation once — BitmapFactory ignores it.
    val exifOrient = readExifOrientation(path)

    // For orientations that involve a 90°/270° rotation, the raw pixel axes are
    // transposed relative to the final image. Swap reqW/reqH when computing
    // inSampleSize so we preserve enough pixels in both final dimensions.
    val swapDims = exifOrient == ExifInterface.ORIENTATION_ROTATE_90 ||
            exifOrient == ExifInterface.ORIENTATION_ROTATE_270 ||
            exifOrient == ExifInterface.ORIENTATION_TRANSPOSE ||
            exifOrient == ExifInterface.ORIENTATION_TRANSVERSE
    val logicalReqW = if (swapDims) reqHeight else reqWidth
    val logicalReqH = if (swapDims) reqWidth else reqHeight

    // Coarse inSampleSize: largest power-of-2 so decoded size ≥ target
    val sampleSize = calcInSampleSize(srcW, srcH, logicalReqW, logicalReqH, centerCrop)

    // Dimensions after coarse subsample
    val sampledW = (srcW + sampleSize - 1) / sampleSize
    val sampledH = (srcH + sampleSize - 1) / sampleSize

    // Density-based fractional scale: BitmapFactory performs this in native
    // code during decode — no createScaledBitmap() needed.
    val scaleFactor = if (centerCrop) {
        maxOf(logicalReqW.toFloat() / sampledW, logicalReqH.toFloat() / sampledH)
    } else {
        minOf(logicalReqW.toFloat() / sampledW, logicalReqH.toFloat() / sampledH)
    }.coerceAtMost(1f) // never upscale in density pass

    // Pass 2 — decode with both subsample + scale in one native call
    opts.inJustDecodeBounds = false
    opts.inSampleSize = sampleSize
    opts.inPreferredConfig = Bitmap.Config.ARGB_8888
    if (scaleFactor < 1f) {
        val densityBase = 10_000
        opts.inDensity = densityBase
        opts.inTargetDensity = (densityBase * scaleFactor + 0.5f).toInt()
        opts.inScaled = true
    }

    val decoded = BitmapFactory.decodeFile(path, opts) ?: return null

    // Apply EXIF orientation (rotate + optional flip) BEFORE crop so the crop
    // operates on the correctly-oriented image.
    val oriented = applyExifOrientation(decoded, exifOrient)

    // Crop (centerCrop) or return as-is (FIT)
    val result = if (centerCrop) cropCenter(oriented, reqWidth, reqHeight) else oriented

    // Sharpen only small thumbnails — pixel loop cost scales with pixel count:
    // 200×200 = 40 K px ≈ 5 ms OK; 1024×1024 = 1 M px ≈ 150 ms too slow.
    return if (result.width <= SHARPEN_THRESHOLD && result.height <= SHARPEN_THRESHOLD) {
        result.applySharpen()
    } else {
        result
    }
}

/**
 * Read the raw EXIF orientation tag from the file.
 * Returns [ExifInterface.ORIENTATION_NORMAL] (1) on any error.
 */
internal fun readExifOrientation(path: String): Int = try {
    ExifInterface(path).getAttributeInt(
        ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
    )
} catch (_: Exception) { ExifInterface.ORIENTATION_NORMAL }

/**
 * Apply the full EXIF orientation transform (rotate + optional flip) to [bitmap].
 *
 * Handles all 8 EXIF orientation values per the TIFF/EXIF specification:
 *
 *  Value | Name               | Transform needed to display correctly
 *  ------|--------------------|--------------------------------------
 *    1   | NORMAL             | none
 *    2   | FLIP_HORIZONTAL    | flip X
 *    3   | ROTATE_180         | rotate 180°
 *    4   | FLIP_VERTICAL      | flip Y
 *    5   | TRANSPOSE          | rotate 90° CW + flip X
 *    6   | ROTATE_90          | rotate 90° CW
 *    7   | TRANSVERSE         | rotate 270° CW + flip X
 *    8   | ROTATE_270         | rotate 270° CW
 *
 * The original [bitmap] is recycled if a new one is created.
 * Returns [bitmap] unchanged for NORMAL / UNDEFINED.
 */
internal fun applyExifOrientation(bitmap: Bitmap, orient: Int): Bitmap {
    val m = Matrix()
    when (orient) {
        ExifInterface.ORIENTATION_NORMAL,
        ExifInterface.ORIENTATION_UNDEFINED -> return bitmap

        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> m.postScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> { m.postRotate(90f); m.preScale(-1f, 1f) }
        ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
        ExifInterface.ORIENTATION_TRANSVERSE -> { m.postRotate(270f); m.preScale(-1f, 1f) }
        ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
        else -> return bitmap
    }
    val result = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
    if (result !== bitmap) bitmap.recycle()
    return result
}

/**
 * Largest power-of-2 inSampleSize such that the decoded image is still ≥
 * the target in the scaling-relevant dimension.
 * - FILL (centerCrop): need both dimensions ≥ target → keep the max side.
 * - FIT: need at least one dimension ≥ target → keep the min side.
 */
private fun calcInSampleSize(srcW: Int, srcH: Int, reqW: Int, reqH: Int, fill: Boolean): Int {
    var size = 1
    while (true) {
        val next = size * 2
        val nw = srcW / next
        val nh = srcH / next
        val fits = if (fill) nw >= reqW && nh >= reqH else nw >= reqW || nh >= reqH
        if (!fits || nw <= 0 || nh <= 0) break
        size = next
    }
    return size
}

/** Center-crop decoded bitmap to exactly reqW × reqH. Recycles the original. */
private fun cropCenter(bitmap: Bitmap, reqW: Int, reqH: Int): Bitmap {
    val w = bitmap.width
    val h = bitmap.height
    if (w == reqW && h == reqH) return bitmap
    val x = ((w - reqW) / 2).coerceAtLeast(0)
    val y = ((h - reqH) / 2).coerceAtLeast(0)
    val cropped = Bitmap.createBitmap(bitmap, x, y, minOf(reqW, w), minOf(reqH, h))
    if (cropped !== bitmap) bitmap.recycle()
    return cropped
}

/**
 * Apply a 3×3 unsharp-mask sharpen kernel using integer-scaled arithmetic.
 *
 * Kernel: [ 0, −f, 0; −f, 1+4f, −f; 0, −f, 0 ]  (sum = 1, identity-preserving)
 *
 * Only call this on small bitmaps (≤ SHARPEN_THRESHOLD px per side) to stay
 * under 10 ms. Recycles the receiver and returns a new bitmap.
 */
fun Bitmap.applySharpen(factor: Float = 0.25f): Bitmap {
    val w = width
    val h = height
    if (w < 3 || h < 3) return this

    val src = IntArray(w * h)
    getPixels(src, 0, w, 0, 0, w, h)
    val dst = src.copyOf()

    val iCenter = ((1f + 4f * factor) * 256f + 0.5f).toInt()
    val iSide = -((iCenter - 256 + 2) / 4)

    for (y in 1 until h - 1) {
        val row = y * w
        for (x in 1 until w - 1) {
            val i = row + x
            val c = src[i]; val n = src[i - w]; val s = src[i + w]
            val e = src[i + 1]; val ww = src[i - 1]

            val rSum = (n shr 16 and 0xFF) + (s shr 16 and 0xFF) + (e shr 16 and 0xFF) + (ww shr 16 and 0xFF)
            val gSum = (n shr 8 and 0xFF) + (s shr 8 and 0xFF) + (e shr 8 and 0xFF) + (ww shr 8 and 0xFF)
            val bSum = (n and 0xFF) + (s and 0xFF) + (e and 0xFF) + (ww and 0xFF)

            val r = ((c shr 16 and 0xFF) * iCenter + rSum * iSide shr 8).coerceIn(0, 255)
            val g = ((c shr 8 and 0xFF) * iCenter + gSum * iSide shr 8).coerceIn(0, 255)
            val b = ((c and 0xFF) * iCenter + bSum * iSide shr 8).coerceIn(0, 255)

            dst[i] = (c ushr 24 shl 24) or (r shl 16) or (g shl 8) or b
        }
    }

    val result = Bitmap.createBitmap(w, h, config ?: Bitmap.Config.ARGB_8888)
    result.setPixels(dst, 0, w, 0, 0, w, h)
    recycle()
    return result
}
