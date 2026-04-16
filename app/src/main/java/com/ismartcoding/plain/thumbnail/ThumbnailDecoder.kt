package com.ismartcoding.plain.thumbnail

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build

/**
 * Fast single-pass image decode using the Glide-style density trick.
 *
 * Strategy:
 *  1. inJustDecodeBounds — read source dimensions, no pixel allocation.
 *  2. inSampleSize — coarse power-of-2 subsample so the native decoder loads
 *     only a fraction of the source pixels.
 *  3. inDensity / inTargetDensity — precise fractional scale applied *inside*
 *     the native decoder (libjpeg-turbo folds this into its IDCT stage for
 *     JPEG), eliminating a second Bitmap allocation from createScaledBitmap().
 *  4. Conditional sharpening — only for small outputs (≤ SHARPEN_THRESHOLD px)
 *     where the JVM pixel loop is cheap (< 10 ms). Large outputs (e.g. 1024 px
 *     lightbox) skip sharpening to avoid the ~150 ms overhead.
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

    // Coarse inSampleSize: largest power-of-2 so decoded size ≥ target
    val sampleSize = calcInSampleSize(srcW, srcH, reqWidth, reqHeight, centerCrop)

    // Dimensions after coarse subsample
    val sampledW = (srcW + sampleSize - 1) / sampleSize
    val sampledH = (srcH + sampleSize - 1) / sampleSize

    // Density-based fractional scale: BitmapFactory performs this in native
    // code during decode — no createScaledBitmap() needed.
    val scaleFactor = if (centerCrop) {
        maxOf(reqWidth.toFloat() / sampledW, reqHeight.toFloat() / sampledH)
    } else {
        minOf(reqWidth.toFloat() / sampledW, reqHeight.toFloat() / sampledH)
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

    // Crop (centerCrop) or return as-is (FIT)
    val result = if (centerCrop) cropCenter(decoded, reqWidth, reqHeight) else decoded

    // Sharpen only small thumbnails — pixel loop cost scales with pixel count:
    // 200×200 = 40 K px ≈ 5 ms OK; 1024×1024 = 1 M px ≈ 150 ms too slow.
    return if (result.width <= SHARPEN_THRESHOLD && result.height <= SHARPEN_THRESHOLD) {
        result.applySharpen()
    } else {
        result
    }
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
