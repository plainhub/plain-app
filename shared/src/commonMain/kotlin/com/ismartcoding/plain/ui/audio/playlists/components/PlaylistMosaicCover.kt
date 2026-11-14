package com.ismartcoding.plain.ui.audio.playlists.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import com.ismartcoding.plain.ui.platform.media.loadAudioCoverBitmap
import com.ismartcoding.plain.ui.audio.components.PlaylistCoverArtwork
import com.ismartcoding.plain.ui.audio.components.audioCoverCache
import com.ismartcoding.plain.models.AudioAlbumCover

/**
 * Playlist cover stitched from the embedded art of the playlist's richest
 * albums: up to four covers tile 2×2 into a square; fewer covers fill the
 * square symmetrically (1 full, 2 halves, 3 half + quarters); no cover at
 * all falls back to the gradient placeholder.
 */
@Composable
fun PlaylistMosaicCover(
    albums: List<AudioAlbumCover>,
    gradientIndex: Int,
    modifier: Modifier = Modifier,
    iconSize: Int = 28,
) {
    val source = com.ismartcoding.plain.ui.navigation.LocalContentSource.current
    var covers by remember(albums, source.key) { mutableStateOf<List<ImageBitmap>>(emptyList()) }
    LaunchedEffect(albums, source.key) {
        val found = mutableListOf<ImageBitmap>()
        for (album in albums) {
            if (found.size >= TILES) break
            for (path in album.candidatePaths) {
                val cacheKey = source.key + ":" + path
                if (!audioCoverCache.containsKey(cacheKey)) {
                    audioCoverCache[cacheKey] = withIO { loadAudioCoverBitmap(path, source) }
                }
                val bitmap = audioCoverCache[cacheKey] ?: continue
                found.add(bitmap)
                break
            }
        }
        covers = found
    }

    if (covers.isEmpty()) {
        PlaylistCoverArtwork(gradientIndex = gradientIndex, modifier = modifier, iconSize = iconSize)
        return
    }
    Box(modifier = modifier) {
        when (covers.size) {
            1 -> MosaicTile(covers[0], Modifier.fillMaxSize())
            2 -> Row(Modifier.fillMaxSize()) {
                MosaicTile(covers[0], Modifier.weight(1f).fillMaxHeight())
                MosaicTile(covers[1], Modifier.weight(1f).fillMaxHeight())
            }
            3 -> Row(Modifier.fillMaxSize()) {
                MosaicTile(covers[0], Modifier.weight(1f).fillMaxHeight())
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    MosaicTile(covers[1], Modifier.weight(1f).fillMaxSize())
                    MosaicTile(covers[2], Modifier.weight(1f).fillMaxSize())
                }
            }
            else -> Column(Modifier.fillMaxSize()) {
                Row(Modifier.weight(1f).fillMaxSize()) {
                    MosaicTile(covers[0], Modifier.weight(1f).fillMaxHeight())
                    MosaicTile(covers[1], Modifier.weight(1f).fillMaxHeight())
                }
                Row(Modifier.weight(1f).fillMaxSize()) {
                    MosaicTile(covers[2], Modifier.weight(1f).fillMaxHeight())
                    MosaicTile(covers[3], Modifier.weight(1f).fillMaxHeight())
                }
            }
        }
    }
}

@Composable
private fun MosaicTile(bitmap: ImageBitmap, modifier: Modifier) {
    Image(
        bitmap = bitmap,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier,
    )
}

private const val TILES = 4
