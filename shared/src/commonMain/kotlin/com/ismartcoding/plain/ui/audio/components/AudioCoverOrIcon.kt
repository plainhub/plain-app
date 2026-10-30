package com.ismartcoding.plain.ui.audio.components

import com.ismartcoding.plain.i18n.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.ui.platform.media.loadAudioCoverBitmap
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.music2 as ui_drawable_music2

/**
 * In-memory cache of audio cover art keyed by file path.
 * A null value indicates the audio file was inspected and has no embedded cover.
 * A missing key indicates the path has not yet been inspected.
 * Loaded concurrently from multiple LaunchedEffect(withIO) blocks, so a plain
 * mutableMapOf (LinkedHashMap) is not safe here.
 */
val audioCoverCache = mutableStateMapOf<String, ImageBitmap?>()

@Composable
fun AudioCoverOrIcon(
    path: String?,
    modifier: Modifier = Modifier
) {
    val source = com.ismartcoding.plain.ui.navigation.LocalContentSource.current
    val cacheKey = source.key + ":" + path
    if (path.isNullOrBlank()) {
        Icon(
            painter = painterResource(UiRes.drawable.ui_drawable_music2),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        return
    }

    var imageBitmap by remember(cacheKey) {
        mutableStateOf<ImageBitmap?>(audioCoverCache[cacheKey])
    }
    var isLoading by remember(cacheKey) {
        mutableStateOf(!audioCoverCache.containsKey(cacheKey))
    }

    LaunchedEffect(cacheKey) {
        if (audioCoverCache.containsKey(cacheKey)) {
            imageBitmap = audioCoverCache[cacheKey]
            isLoading = false
            return@LaunchedEffect
        }

        isLoading = true
        val bitmap = withIO { loadAudioCoverBitmap(path, source) }

        audioCoverCache[cacheKey] = bitmap
        imageBitmap = bitmap
        isLoading = false
    }

    if (imageBitmap != null) {
        Image(
            bitmap = imageBitmap!!,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(40.dp)
                .clip(RoundedCornerShape(6.dp))
        )
    } else {
        Icon(
            painter = painterResource(UiRes.drawable.ui_drawable_music2),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}
