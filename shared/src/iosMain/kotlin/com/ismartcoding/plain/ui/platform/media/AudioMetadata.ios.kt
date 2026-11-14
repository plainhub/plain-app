package com.ismartcoding.plain.ui.platform.media

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import com.ismartcoding.plain.lib.toByteArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image
import platform.AVFoundation.AVMetadataCommonKeyArtwork
import platform.AVFoundation.AVMetadataItem
import platform.AVFoundation.commonKey
import platform.AVFoundation.commonMetadata
import platform.AVFoundation.dataValue
import platform.AVFoundation.AVURLAsset
import platform.Foundation.NSURL

internal actual suspend fun decodeLocalAudioCover(path: String): ImageBitmap? = withContext(Dispatchers.Default) {
    val url = if (path.startsWith("file://")) NSURL.URLWithString(path) ?: return@withContext null
    else NSURL.fileURLWithPath(path)
    val asset = AVURLAsset(url, null)
    val bytes = asset.commonMetadata().filterIsInstance<AVMetadataItem>()
        .firstOrNull { it.commonKey() == AVMetadataCommonKeyArtwork }
        ?.dataValue()?.toByteArray() ?: return@withContext null
    val image = Image.makeFromEncoded(bytes)
    try {
        Bitmap.makeFromImage(image).asComposeImageBitmap()
    } finally {
        image.close()
    }
}
