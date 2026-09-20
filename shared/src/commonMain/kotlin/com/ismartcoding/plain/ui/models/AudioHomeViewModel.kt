package com.ismartcoding.plain.ui.models

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.ismartcoding.plain.audio.DAudio
import com.ismartcoding.plain.db.DAudioPlaylist
import com.ismartcoding.plain.features.audio.AudioQueueManager
import com.ismartcoding.plain.ui.page.playlist.PlaylistAlbumCover
import com.ismartcoding.plain.ui.page.playlist.loadPlaylistAlbumCovers

data class AudioHomeArtist(val name: String, val itemCount: Int, val playCount: Long, val samplePath: String)

class AudioHomeViewModel : ViewModel() {
    val playlists = mutableStateOf<List<Pair<DAudioPlaylist, Int>>>(listOf())

    /** Playlist id -> album covers feeding the home mosaic tiles. */
    val playlistCovers = mutableStateOf<Map<String, List<PlaylistAlbumCover>>>(emptyMap())

    /** 最近 section: play history resolved to library tracks, or recently-added as fallback. */
    val recentItems = mutableStateOf<List<DAudio>>(listOf())

    /** All artists, most played first, then by name. */
    val artists = mutableStateOf<List<AudioHomeArtist>>(listOf())

    suspend fun loadAsync(audioVM: AudioViewModel) {
        playlists.value = AudioQueueManager.playlists()
        rebuild(audioVM.itemsFlow.value)
        playlistCovers.value = loadPlaylistCovers()
    }

    private suspend fun loadPlaylistCovers(): Map<String, List<PlaylistAlbumCover>> {
        val covers = mutableMapOf<String, List<PlaylistAlbumCover>>()
        for ((pl, count) in playlists.value) {
            if (count == 0) continue
            val items = AudioQueueManager.playlistItemsPage(pl.id, 0, PLAYLIST_ITEMS_LIMIT)
            if (items.isEmpty()) continue
            covers[pl.id] = loadPlaylistAlbumCovers(items)
        }
        return covers
    }

    suspend fun rebuild(items: List<DAudio>) {
        val history = AudioQueueManager.recentPage(limit = 8, offset = 0)
        recentItems.value = if (history.isNotEmpty()) {
            val byPath = items.associateBy { it.path }
            history.mapNotNull { byPath[it.path] }.ifEmpty {
                items.sortedByDescending { it.createdAt }.take(8)
            }
        } else {
            items.sortedByDescending { it.createdAt }.take(8)
        }
        val playCounts = AudioQueueManager.artistPlayCounts()
        artists.value = items
            .filter { it.artist.isNotBlank() }
            .groupBy { it.artist }
            .map { (name, list) ->
                AudioHomeArtist(name = name, itemCount = list.size, playCount = playCounts[name] ?: 0, samplePath = list.first().path)
            }
            .sortedWith(compareByDescending<AudioHomeArtist> { it.playCount }.thenBy { it.name })
    }
}

private const val PLAYLIST_ITEMS_LIMIT = 1000
