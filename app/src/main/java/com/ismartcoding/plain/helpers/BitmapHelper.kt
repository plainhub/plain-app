package com.ismartcoding.plain.helpers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.ismartcoding.plain.extensions.getBitmapAsync
import java.io.File

object BitmapHelper {
    suspend fun decodeBitmapFromFileAsync(
        context: Context,
        path: String,
        reqWidth: Int,
        reqHeight: Int,
    ): Bitmap? {
        return File(path).getBitmapAsync(context, reqWidth, reqHeight)
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
