package com.ismartcoding.plain.ui.platform.media

import androidx.compose.ui.graphics.ImageBitmap
import com.ismartcoding.plain.features.media.MediaClient
import com.ismartcoding.plain.lib.logcat.LogCat
import kotlinx.coroutines.CancellationException

suspend fun loadAudioCoverBitmap(path: String, source: com.ismartcoding.plain.rust.graphql.ContentSource = com.ismartcoding.plain.rust.graphql.ContentSource.Local): ImageBitmap? = try {
    val cached = source.media.cacheAudioCover(path)
    cached?.let {
        when (source) {
            com.ismartcoding.plain.rust.graphql.ContentSource.Local -> decodeImageFileBitmap(it)
            is com.ismartcoding.plain.rust.graphql.ContentSource.Remote -> decodeManagedImageBitmap(source.client.resource(it))
        }
    }
} catch (error: CancellationException) {
    throw error
} catch (error: Exception) {
    LogCat.e("Audio cover load failed: ${error.message}")
    null
}
