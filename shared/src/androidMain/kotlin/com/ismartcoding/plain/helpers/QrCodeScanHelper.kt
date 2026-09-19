package com.ismartcoding.plain.helpers

import android.graphics.Bitmap
import com.ismartcoding.plain.platform.ScannedCode
import com.ismartcoding.plain.platform.ScannedImage
import com.ismartcoding.plain.lib.extensions.scaleDown

object QrCodeScanHelper {
    // decode largest first; tiny down-scales misdecode dense codes, so the ladder stops at 900
    private val LADDER = intArrayOf(1600, 900)

    /** Returns null when no code is found; positions are normalized to the decoded bitmap. */
    fun decodeAll(source: Bitmap): ScannedImage? {
        val reader = QrScanPipeline.createReader()
        val sourceMax = maxOf(source.width, source.height)
        for (maxDim in LADDER) {
            if (sourceMax <= maxDim && maxDim != LADDER[0]) continue
            val bitmap = if (sourceMax > maxDim) source.scaleDown(maxDim) else source
            val luminance = bitmapToLuminance(bitmap)
            val codes = QrScanPipeline.decode(reader, luminance, bitmap.width, bitmap.height)
            if (codes.isNotEmpty()) {
                val w = bitmap.width.toFloat()
                val h = bitmap.height.toFloat()
                return ScannedImage(
                    codes.map { ScannedCode(it.text, it.centerX / w, it.centerY / h) },
                    bitmap.width,
                    bitmap.height,
                )
            }
        }
        return null
    }

    fun tryDecode(source: Bitmap): String? = decodeAll(source)?.codes?.firstOrNull()?.text

    private fun bitmapToLuminance(source: Bitmap): ByteArray {
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)
        val luminance = ByteArray(width * height)
        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (p shr 16) and 0xff
            val g = (p shr 8) and 0xff
            val b = p and 0xff
            luminance[i] = ((r * 299 + g * 587 + b * 114) / 1000).toByte()
        }
        return luminance
    }
}
