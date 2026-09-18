package com.ismartcoding.plain.ui.page.audio.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.platform.loadAudioCoverBitmap
import org.jetbrains.compose.resources.painterResource

/**
 * Artist avatar: the artist's album artwork cropped to a circle — the standard
 * fallback chain in local music players (Plex, Jellyfin, Retro Music). Artists
 * without embedded cover art get a tonal Material person icon.
 */
@Composable
fun ArtistAvatar(name: String, artworkPath: String?, size: Dp, modifier: Modifier = Modifier) {
    var cover by remember(artworkPath) { mutableStateOf<ImageBitmap?>(artworkPath?.let { audioCoverCache[it] }) }
    LaunchedEffect(artworkPath) {
        val key = artworkPath ?: return@LaunchedEffect
        if (!audioCoverCache.containsKey(key)) {
            audioCoverCache[key] = withIO { loadAudioCoverBitmap(key) }
        }
        cover = audioCoverCache[key]
    }
    val bitmap = cover
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = name,
            contentScale = ContentScale.Crop,
            modifier = modifier.size(size).clip(CircleShape),
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(Res.drawable.person),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(size * 0.55f),
            )
        }
    }
}
