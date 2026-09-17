package com.ismartcoding.plain.ui.models

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.ismartcoding.plain.audio.DAudio
import com.ismartcoding.plain.db.DAudioPlaylist
import com.ismartcoding.plain.features.audio.AudioQueueManager

data class AudioHomeArtist(val name: String, val songCount: Int, val playCount: Long)

class AudioHomeViewModel : ViewModel() {
    val playlists = mutableStateOf<List<Pair<DAudioPlaylist, Int>>>(listOf())

    /** 最近 section: play history resolved to library tracks, or recently-added as fallback. */
    val recentSongs = mutableStateOf<List<DAudio>>(listOf())

    /** All artists, most played first, then by name. */
    val artists = mutableStateOf<List<AudioHomeArtist>>(listOf())

    suspend fun loadAsync(audioVM: AudioViewModel) {
        playlists.value = AudioQueueManager.playlists()
        rebuild(audioVM.itemsFlow.value)
    }

    suspend fun rebuild(songs: List<DAudio>) {
        val history = AudioQueueManager.recentPage(limit = 8, offset = 0)
        recentSongs.value = if (history.isNotEmpty()) {
            val byPath = songs.associateBy { it.path }
            history.mapNotNull { byPath[it.path] }.ifEmpty {
                songs.sortedByDescending { it.createdAt }.take(8)
            }
        } else {
            songs.sortedByDescending { it.createdAt }.take(8)
        }
        val playCounts = AudioQueueManager.artistPlayCounts()
        artists.value = songs
            .filter { it.artist.isNotBlank() }
            .groupBy { it.artist }
            .map { (name, list) ->
                AudioHomeArtist(name = name, songCount = list.size, playCount = playCounts[name] ?: 0)
            }
            .sortedWith(compareByDescending<AudioHomeArtist> { it.playCount }.thenBy { it.name })
    }
}
