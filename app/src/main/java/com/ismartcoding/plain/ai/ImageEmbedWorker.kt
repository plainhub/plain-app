package com.ismartcoding.plain.ai

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import com.google.ai.edge.litert.CompiledModel
import com.google.ai.edge.litert.TensorBuffer
import com.ismartcoding.lib.logcat.LogCat
import java.io.File

/**
 * Independent inference worker with its own model instance and buffers.
 * Multiple workers can run in parallel for high-throughput indexing.
 */
class ImageEmbedWorker(modelFile: File, private val inputSize: Int = 256) : AutoCloseable {
    private val model: CompiledModel = DelegateHelper.createModel(modelFile)
    private val inputBuffers: List<TensorBuffer> = model.createInputBuffers()
    private val outputBuffers: List<TensorBuffer> = model.createOutputBuffers()
    private val pixelsBuf = IntArray(inputSize * inputSize)
    private val chwBuf = FloatArray(3 * inputSize * inputSize)

    fun embedBitmap(bitmap: Bitmap): FloatArray? {
        return try {
            bitmapToNCHW(bitmap, pixelsBuf, chwBuf)
            inputBuffers[0].writeFloat(chwBuf)
            model.run(inputBuffers, outputBuffers)
            val emb = outputBuffers[0].readFloat()
            if (hasInvalidValues(emb)) null else l2Normalize(emb)
        } catch (e: Exception) {
            LogCat.e("ImageEmbedWorker: inference failed", e)
            null
        } finally {
            bitmap.recycle()
        }
    }

    override fun close() {
        model.close()
    }

    private fun bitmapToNCHW(bitmap: Bitmap, pixels: IntArray, chw: FloatArray) {
        bitmap.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)
        val planeSize = inputSize * inputSize
        for (i in 0 until planeSize) {
            val p = pixels[i]
            chw[i] = (p shr 16 and 0xFF) / 255f
            chw[planeSize + i] = (p shr 8 and 0xFF) / 255f
            chw[2 * planeSize + i] = (p and 0xFF) / 255f
        }
    }

    companion object {
        fun loadBitmap(imagePath: String, inputSize: Int = 256): Bitmap? {
            return loadAndResize(imagePath, inputSize)
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
}
