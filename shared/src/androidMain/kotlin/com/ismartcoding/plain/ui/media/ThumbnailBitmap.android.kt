package com.ismartcoding.plain.ui.media

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

internal actual fun thumbnailBitmap(pixels: ThumbHashPixels): ImageBitmap {
    val argb = IntArray(pixels.width * pixels.height) { index ->
        val start = index * 4
        val rgba = pixels.rgba
        ((rgba[start + 3].toInt() and 255) shl 24) or
            ((rgba[start].toInt() and 255) shl 16) or
            ((rgba[start + 1].toInt() and 255) shl 8) or (rgba[start + 2].toInt() and 255)
    }
    return Bitmap.createBitmap(argb, pixels.width, pixels.height, Bitmap.Config.ARGB_8888).asImageBitmap()
}
