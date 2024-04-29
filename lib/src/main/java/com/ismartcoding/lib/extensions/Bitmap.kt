package com.ismartcoding.lib.extensions

import android.graphics.Bitmap
import com.ismartcoding.lib.isRPlus
import java.io.ByteArrayOutputStream

fun Bitmap.scaleDown(maxSize: Int): Bitmap {
    val source = this
    val height: Float
    val width: Float
    if (source.width == 0 || source.width == 0) {
        height = maxSize.toFloat()
        width = maxSize.toFloat()
    } else if (source.width > source.height) {
        height = source.height.toFloat() / source.width.toFloat() * maxSize.toFloat()
        width = maxSize.toFloat()
    } else {
        width = source.width.toFloat() / source.height.toFloat() * maxSize.toFloat()
        height = maxSize.toFloat()
    }
    return Bitmap.createScaledBitmap(source, width.toInt(), height.toInt(), true)
}

@Suppress("DEPRECATION")
fun Bitmap.compress(quality: Int, outputStream: ByteArrayOutputStream) {
    if (isRPlus()) {
        compress(Bitmap.CompressFormat.WEBP_LOSSY, quality, outputStream)
    } else {
        compress(Bitmap.CompressFormat.WEBP, quality, outputStream)
    }
}