package com.ismartcoding.plain.ui.platform.media

import androidx.compose.ui.graphics.ImageBitmap
import com.ismartcoding.plain.features.chat.peer.ResourceSize
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.rust.graphql.ContentSource
import kotlinx.coroutines.CancellationException

internal expect suspend fun decodeLocalAudioCover(path: String): ImageBitmap?

suspend fun loadAudioCoverBitmap(path: String, source: ContentSource = ContentSource.Local): ImageBitmap? = try {
    when (source) {
        ContentSource.Local -> decodeLocalAudioCover(path)
        is ContentSource.Remote -> decodeManagedImageBitmap(source.client.resource(path, ResourceSize.LG))
    }
} catch (error: CancellationException) {
    throw error
} catch (error: Exception) {
    LogCat.e("Audio cover load failed: ${error.message}")
    null
}
