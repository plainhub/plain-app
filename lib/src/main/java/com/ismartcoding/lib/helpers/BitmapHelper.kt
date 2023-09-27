package com.ismartcoding.lib.helpers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.ismartcoding.lib.extensions.getBitmapAsync
import com.ismartcoding.lib.extensions.isVideoFast
import java.io.File

object BitmapHelper {
    suspend fun decodeBitmapFromFileAsync(
        context: Context,
        filePath: String,
        reqWidth: Int,
        reqHeight: Int,
    ): Bitmap? {
        if (filePath.isVideoFast()) {
            return File(filePath).getBitmapAsync(context, reqWidth, reqHeight)
        }

        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(filePath, options)

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false

        return BitmapFactory.decodeFile(filePath, options)
    }

    fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int,
    ): Int {
        val imageHeight = options.outHeight
        val imageWidth = options.outWidth
        var inSampleSize = 1

        if (imageHeight > reqHeight || imageWidth > reqWidth) {
            val halfHeight = imageHeight / 2
            val halfWidth = imageWidth / 2

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both height and width larger than the requested height and width
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }
}
