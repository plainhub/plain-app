@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.ismartcoding.plain.ui.images.editor

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.size.Size
import coil3.toBitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image

internal actual suspend fun importEditorImage(bytes: ByteArray): ImageBitmap {
    val context = PlatformContext.INSTANCE
    val result = SingletonImageLoader.get(context).execute(ImageRequest.Builder(context).data(bytes).size(Size.ORIGINAL).build())
    return checkNotNull(result.image) { "Unable to decode selected image" }.toBitmap().asComposeImageBitmap()
}

internal actual fun encodeEditorImage(image: ImageBitmap, format: EditorRasterFormat): ByteArray {
    val compression = when (format) {
        EditorRasterFormat.PNG -> EncodedImageFormat.PNG
        EditorRasterFormat.JPEG -> EncodedImageFormat.JPEG
        EditorRasterFormat.WEBP -> EncodedImageFormat.WEBP
    }
    val raster = Image.makeFromBitmap(image.asSkiaBitmap())
    return try {
        val encoded = checkNotNull(raster.encodeToData(compression, 92)) { "Unable to encode editor image" }
        try { encoded.bytes } finally { encoded.close() }
    } finally { raster.close() }
}

internal actual fun publishEditorClipboardImage(path: String) {
    platform.UIKit.UIPasteboard.generalPasteboard.image = checkNotNull(platform.UIKit.UIImage.imageWithContentsOfFile(path)) { "Unable to read clipboard image" }
}
