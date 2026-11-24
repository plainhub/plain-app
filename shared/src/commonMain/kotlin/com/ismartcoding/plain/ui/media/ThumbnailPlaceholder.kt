package com.ismartcoding.plain.ui.media

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.ismartcoding.plain.platform.system.PlatformLock
import com.ismartcoding.plain.ui.base.PPlaceholder

internal object ThumbnailPlaceholders {
    private val lock = PlatformLock()
    private val images = linkedMapOf<String, ImageBitmap>()

    fun get(hash: String): ImageBitmap? = lock.withLock {
        val bitmap = images.remove(hash) ?: decodeThumbHash(hash)?.let(::thumbnailBitmap) ?: return@withLock null
        images[hash] = bitmap
        if (images.size > 128) images.remove(images.keys.first())
        bitmap
    }

    fun clear() = lock.withLock { images.clear() }
}

@Composable
internal fun ThumbnailPlaceholder(thumbHash: String?, modifier: Modifier = Modifier) {
    val image = remember(thumbHash) { thumbHash?.let(ThumbnailPlaceholders::get) }
    if (image == null) {
        PPlaceholder(modifier)
        return
    }
    Image(image, contentDescription = null, modifier = modifier, contentScale = ContentScale.Crop)
}

internal expect fun thumbnailBitmap(pixels: ThumbHashPixels): ImageBitmap
