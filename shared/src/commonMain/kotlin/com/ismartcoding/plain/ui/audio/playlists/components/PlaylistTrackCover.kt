package com.ismartcoding.plain.ui.audio.playlists.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.ui.audio.components.PlaylistCoverArtwork
import com.ismartcoding.plain.ui.audio.components.audioCoverCache
import com.ismartcoding.plain.ui.navigation.LocalContentSource
import com.ismartcoding.plain.ui.platform.media.loadAudioCoverBitmap

@Composable
fun PlaylistTrackCover(
    candidatePaths: List<String>,
    gradientIndex: Int,
    modifier: Modifier = Modifier,
    iconSize: Int = 28,
) {
    val source = LocalContentSource.current
    var cover by remember(candidatePaths, source.key) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(candidatePaths, source.key) {
        for (path in candidatePaths) {
            val cacheKey = source.key + ":" + path
            if (!audioCoverCache.containsKey(cacheKey)) {
                audioCoverCache[cacheKey] = withIO { loadAudioCoverBitmap(path, source) }
            }
            val bitmap = audioCoverCache[cacheKey] ?: continue
            cover = bitmap
            break
        }
    }

    val bitmap = cover
    if (bitmap == null) {
        PlaylistCoverArtwork(gradientIndex = gradientIndex, modifier = modifier, iconSize = iconSize)
    } else {
        Image(bitmap, contentDescription = null, modifier = modifier, contentScale = ContentScale.Crop)
    }
}
