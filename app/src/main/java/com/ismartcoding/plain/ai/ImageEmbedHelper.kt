package com.ismartcoding.plain.ai

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import com.google.ai.edge.litert.CompiledModel
import com.google.ai.edge.litert.TensorBuffer
import com.ismartcoding.lib.logcat.LogCat
import java.io.File

object ImageEmbedHelper {
    private var model: CompiledModel? = null
    private var inputBuffers: List<TensorBuffer>? = null
    private var outputBuffers: List<TensorBuffer>? = null

    // MobileCLIP-S2 uses identity normalization: mean=(0,0,0) std=(1,1,1)
    // Just scale pixels to [0, 1] — no further normalization needed.

    // Reusable buffers to avoid per-image allocation
    private var pixelsBuf: IntArray? = null
    private var chwBuf: FloatArray? = null

    fun init(modelFile: File, inputSize: Int = 256) {
        close()
        val m = DelegateHelper.createModel(modelFile)
        model = m
        inputBuffers = m.createInputBuffers()
        outputBuffers = m.createOutputBuffers()
        // Pre-allocate reusable buffers
        pixelsBuf = IntArray(inputSize * inputSize)
        chwBuf = FloatArray(3 * inputSize * inputSize)
    }

    fun embed(imagePath: String, inputSize: Int = 256): FloatArray? {
        val bitmap = loadBitmap(imagePath, inputSize) ?: return null
        return embedBitmap(bitmap)
    }

    /** Load and preprocess a bitmap for inference (I/O heavy, safe to call from preloader). */
    fun loadBitmap(imagePath: String, inputSize: Int = 256): Bitmap? {
        return loadAndResize(imagePath, inputSize)
    }

    /** Run model inference on a pre-loaded bitmap. Caller must not reuse bitmap after this. */
    fun embedBitmap(bitmap: Bitmap, inputSize: Int = 256): FloatArray? {
        val inBufs = inputBuffers ?: return null
        val outBufs = outputBuffers ?: return null
        return try {
            bitmapToNCHW(bitmap, inputSize, pixelsBuf!!, chwBuf!!)
            inBufs[0].writeFloat(chwBuf!!)
            model!!.run(inBufs, outBufs)
            val emb = outBufs[0].readFloat()
            if (hasInvalidValues(emb)) null else l2Normalize(emb)
        } catch (e: Exception) {
            LogCat.e("ImageEmbedHelper: inference failed", e)
            null
        } finally {
            bitmap.recycle()
        }
    }

    fun close() {
        model?.close(); model = null
        inputBuffers = null; outputBuffers = null
        pixelsBuf = null; chwBuf = null
    }

    private fun bitmapToNCHW(bitmap: Bitmap, size: Int, pixels: IntArray, chw: FloatArray) {
        bitmap.getPixels(pixels, 0, size, 0, 0, size, size)
        val planeSize = size * size
        for (i in 0 until planeSize) {
            val p = pixels[i]
            chw[i] = (p shr 16 and 0xFF) / 255f
            chw[planeSize + i] = (p shr 8 and 0xFF) / 255f
            chw[2 * planeSize + i] = (p and 0xFF) / 255f
        }
    }

    private fun loadAndResize(path: String, inputSize: Int): Bitmap? {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, opts)
        if (opts.outWidth < 64 || opts.outHeight < 64) return null
        val minDim = minOf(opts.outWidth, opts.outHeight)
        var inSampleSize = 1
        while (minDim / (inSampleSize * 2) >= inputSize) inSampleSize *= 2
        opts.inJustDecodeBounds = false
        opts.inSampleSize = inSampleSize
        opts.inPreferredConfig = Bitmap.Config.ARGB_8888
        val raw = BitmapFactory.decodeFile(path, opts) ?: return null
        val rotated = applyExifRotation(raw, path)
        val scale = inputSize.toFloat() / minOf(rotated.width, rotated.height)
        val w = (rotated.width * scale).toInt().coerceAtLeast(inputSize)
        val h = (rotated.height * scale).toInt().coerceAtLeast(inputSize)
        val scaled = Bitmap.createScaledBitmap(rotated, w, h, true)
        if (scaled !== rotated) rotated.recycle()
        return centerCrop(scaled, inputSize)
    }

    private fun applyExifRotation(bitmap: Bitmap, path: String): Bitmap {
        val orient = try {
            ExifInterface(path).getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL,
            )
        } catch (_: Exception) { ExifInterface.ORIENTATION_NORMAL }
        val m = Matrix()
        when (orient) {
            ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
            else -> return bitmap
        }
        val r = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
        if (r !== bitmap) bitmap.recycle()
        return r
    }

    private fun centerCrop(bitmap: Bitmap, size: Int): Bitmap {
        val x = ((bitmap.width - size) / 2).coerceAtLeast(0)
        val y = ((bitmap.height - size) / 2).coerceAtLeast(0)
        val c = Bitmap.createBitmap(bitmap, x, y, size, size)
        if (c !== bitmap) bitmap.recycle()
        return c
    }
}
