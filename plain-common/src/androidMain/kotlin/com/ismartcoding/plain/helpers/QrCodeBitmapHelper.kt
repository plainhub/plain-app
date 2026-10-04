package com.ismartcoding.plain.helpers

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.ismartcoding.plain.lib.logcat.LogCat
import java.io.IOException

object QrCodeBitmapHelper {
    fun getBitmapFromUri(context: Context, imageUri: Uri): Bitmap {
        val source = ImageDecoder.createSource(
            context.contentResolver,
            imageUri
        )
        return ImageDecoder.decodeBitmap(source).copy(Bitmap.Config.ARGB_8888, true)
    }
}
