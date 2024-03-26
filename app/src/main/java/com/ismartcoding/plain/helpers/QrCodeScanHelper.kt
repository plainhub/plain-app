package com.ismartcoding.plain.helpers

import android.graphics.Bitmap
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.RGBLuminanceSource
import java.util.EnumMap
import com.google.zxing.Result
import com.google.zxing.common.HybridBinarizer
import com.ismartcoding.lib.extensions.scaleDown

object QrCodeScanHelper {
    fun tryDecode(source: Bitmap): Result? {
        val reader = createReader()
        val resolutions = intArrayOf(1500, 600, 300, 200, 150, 100)
        for (res in resolutions) {
            val image = source.scaleDown(res)
            try {
                return reader.decodeWithState(getBinaryBitmap(image))
            } catch (e: NotFoundException) {
            }
        }
        return null
    }

    fun createReader(): MultiFormatReader {
        val reader = MultiFormatReader()
        val hintsMap: MutableMap<DecodeHintType, Any> = EnumMap(DecodeHintType::class.java)
        hintsMap[DecodeHintType.TRY_HARDER] = true
        reader.setHints(hintsMap)
        return reader
    }


    private fun getBinaryBitmap(source: Bitmap): BinaryBitmap {
        val width = source.width
        val height = source.height
        val size = width * height

        val bitmapBuffer = IntArray(size)

        source.getPixels(bitmapBuffer, 0, width, 0, 0, width, height)
        val luminanceSource = RGBLuminanceSource(width, height, bitmapBuffer)
        return BinaryBitmap(HybridBinarizer(luminanceSource))
    }
}