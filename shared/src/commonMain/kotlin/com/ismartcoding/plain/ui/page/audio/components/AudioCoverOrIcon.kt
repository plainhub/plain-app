package com.ismartcoding.plain.ui.page.audio.components

import com.ismartcoding.plain.i18n.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.platform.loadAudioCoverBitmap

/**
 * In-memory cache of audio cover art keyed by file path.
 * A null value indicates the audio file was inspected and has no embedded cover.
 * A missing key indicates the path has not yet been inspected.
 */
val audioCoverCache = mutableMapOf<String, ImageBitmap?>()

@Composable
fun AudioCoverOrIcon(
    path: String?,
    modifier: Modifier = Modifier
) {
    if (path.isNullOrBlank()) {
        Icon(
            painter = painterResource(Res.drawable.music2),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        return
    }

    var imageBitmap by remember(path) {
        mutableStateOf<ImageBitmap?>(audioCoverCache[path])
    }
    var isLoading by remember(path) {
        mutableStateOf(!audioCoverCache.containsKey(path))
    }

    LaunchedEffect(path) {
        if (audioCoverCache.containsKey(path)) {
            imageBitmap = audioCoverCache[path]
            isLoading = false
            return@LaunchedEffect
        }

        isLoading = true
        val bitmap = withIO { loadAudioCoverBitmap(path) }

        audioCoverCache[path] = bitmap
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
            painter = painterResource(Res.drawable.music2),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}
