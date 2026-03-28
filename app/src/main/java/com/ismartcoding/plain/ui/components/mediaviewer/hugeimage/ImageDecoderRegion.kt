package com.ismartcoding.plain.ui.components.mediaviewer.hugeimage

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Matrix
import android.graphics.Rect
import com.ismartcoding.plain.enums.RotationType

internal fun buildRenderBlockList(
    countH: Int,
    countW: Int,
    blockSize: Int,
    decoderWidth: Int,
    decoderHeight: Int,
): Array<Array<RenderBlock>> {
    var endX: Int
    var endY: Int
    var sliceStartX: Int
    var sliceStartY: Int
    var sliceEndX: Int
    var sliceEndY: Int
    return Array(countH) { column ->
        sliceStartY = (column * blockSize)
        endY = (column + 1) * blockSize
        sliceEndY = if (endY > decoderHeight) decoderHeight else endY
        Array(countW) { row ->
            sliceStartX = (row * blockSize)
            endX = (row + 1) * blockSize
            sliceEndX = if (endX > decoderWidth) decoderWidth else endX
            RenderBlock(
                sliceRect = Rect(
                    sliceStartX,
                    sliceStartY,
                    sliceEndX,
                    sliceEndY,
                )
            )
        }
    }
}

internal fun getRotateBitmap(bitmap: Bitmap, degree: Float): Bitmap {
    val matrix = Matrix()
    matrix.postRotate(degree)
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, false)
}

/**
 * 解码渲染区域
 */
internal fun decodeRegionBitmap(
    decoder: BitmapRegionDecoder,
    rotation: Int,
    decoderWidth: Int,
    decoderHeight: Int,
    inSampleSize: Int,
    rect: Rect,
): Bitmap? {
    synchronized(decoder) {
        return try {
            val ops = BitmapFactory.Options()
            ops.inSampleSize = inSampleSize
            if (decoder.isRecycled) return null
            return if (rotation == RotationType.ROTATION_0.value) {
                decoder.decodeRegion(rect, ops)
            } else {
                val newRect = when (rotation) {
                    RotationType.ROTATION_90.value -> {
                        val nextX1 = rect.top
                        val nextX2 = rect.bottom
                        val nextY1 = decoderWidth - rect.right
                        val nextY2 = decoderWidth - rect.left
                        Rect(nextX1, nextY1, nextX2, nextY2)
                    }

                    RotationType.ROTATION_180.value -> {
                        val nextX1 = decoderWidth - rect.right
                        val nextX2 = decoderWidth - rect.left
                        val nextY1 = decoderHeight - rect.bottom
                        val nextY2 = decoderHeight - rect.top
                        Rect(nextX1, nextY1, nextX2, nextY2)
                    }

                    RotationType.ROTATION_270.value -> {
                        val nextX1 = decoderHeight - rect.bottom
                        val nextX2 = decoderHeight - rect.top
                        val nextY1 = rect.left
                        val nextY2 = rect.right
                        Rect(nextX1, nextY1, nextX2, nextY2)
                    }

                    else -> throw RotationIllegalException()
                }
                val srcBitmap = decoder.decodeRegion(newRect, ops)
                getRotateBitmap(bitmap = srcBitmap, rotation.toFloat())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
