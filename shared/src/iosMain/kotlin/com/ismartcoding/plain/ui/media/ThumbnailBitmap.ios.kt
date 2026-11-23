package com.ismartcoding.plain.ui.media

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo

internal actual fun thumbnailBitmap(pixels: ThumbHashPixels): ImageBitmap {
    val info = ImageInfo(pixels.width, pixels.height, ColorType.RGBA_8888, ColorAlphaType.UNPREMUL)
    val image = Image.makeRaster(info, pixels.rgba, info.minRowBytes)
    try {
        return Bitmap.makeFromImage(image).asComposeImageBitmap()
    } finally {
        image.close()
    }
}
