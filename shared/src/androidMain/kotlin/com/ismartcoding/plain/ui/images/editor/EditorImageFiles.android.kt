package com.ismartcoding.plain.ui.images.editor

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.size.Size
import coil3.toBitmap
import com.ismartcoding.plain.appContext
import java.io.ByteArrayOutputStream

internal actual suspend fun importEditorImage(bytes: ByteArray): ImageBitmap {
    val result = SingletonImageLoader.get(appContext).execute(ImageRequest.Builder(appContext).data(bytes).size(Size.ORIGINAL).allowHardware(false).build())
    return checkNotNull(result.image) { "Unable to decode selected image" }.toBitmap().asImageBitmap()
}

internal actual fun encodeEditorImage(image: ImageBitmap, format: EditorRasterFormat): ByteArray = ByteArrayOutputStream().use { output ->
    val compression = when (format) {
        EditorRasterFormat.PNG -> Bitmap.CompressFormat.PNG
        EditorRasterFormat.JPEG -> Bitmap.CompressFormat.JPEG
        EditorRasterFormat.WEBP -> Bitmap.CompressFormat.WEBP
    }
    check(image.asAndroidBitmap().compress(compression, 92, output)) { "Unable to encode editor image" }
    output.toByteArray()
}

internal actual fun publishEditorClipboardImage(path: String) {
    val uri = androidx.core.content.FileProvider.getUriForFile(appContext, appContext.packageName + ".provider", java.io.File(path))
    com.ismartcoding.plain.clipboardManager.setPrimaryClip(android.content.ClipData.newUri(appContext.contentResolver, "Image", uri))
}
