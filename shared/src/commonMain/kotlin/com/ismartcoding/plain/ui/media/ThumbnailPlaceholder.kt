package com.ismartcoding.plain.ui.media

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.ismartcoding.plain.ui.base.PPlaceholder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal object ThumbnailPlaceholders {
    private val mutex = Mutex()
    private val images = linkedMapOf<String, ImageBitmap>()

    suspend fun get(hash: String): ImageBitmap? = withContext(Dispatchers.Default) {
        mutex.withLock {
            val bitmap = images.remove(hash) ?: decodeThumbHash(hash)?.let(::thumbnailBitmap)
            if (bitmap != null) {
                images[hash] = bitmap
                if (images.size > 128) images.remove(images.keys.first())
            }
            bitmap
        }
    }

    suspend fun clear() {
        mutex.withLock { images.clear() }
    }
}

@Composable
internal fun ThumbnailPlaceholder(thumbHash: String?, modifier: Modifier = Modifier) {
    val value by produceState<Pair<String, ImageBitmap>?>(null, thumbHash) {
        value = thumbHash?.let { hash -> ThumbnailPlaceholders.get(hash)?.let { hash to it } }
    }
    val image = value?.takeIf { it.first == thumbHash }?.second
    if (image == null) {
        PPlaceholder(modifier)
    } else {
        Image(image, contentDescription = null, modifier = modifier, contentScale = ContentScale.Crop)
    }
}

internal expect fun thumbnailBitmap(pixels: ThumbHashPixels): ImageBitmap
