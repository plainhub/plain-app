package com.ismartcoding.plain.ui.models

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.ismartcoding.plain.audio.DAudio
import com.ismartcoding.plain.db.DAudioPlayHistory
import com.ismartcoding.plain.db.DAudioPlaylist
import com.ismartcoding.plain.features.audio.AudioQueueManager
import com.ismartcoding.plain.lib.withIO

data class AudioHomeArtist(val name: String, val songCount: Int)

class AudioHomeViewModel : ViewModel() {
    val playlists = mutableStateOf<List<Pair<DAudioPlaylist, Int>>>(listOf())
    val recent = mutableStateOf<List<DAudioPlayHistory>>(listOf())

    /** False when there is no play history yet and [recent] holds recently-added tracks instead. */
    val recentIsHistory = mutableStateOf(true)

    val artists = mutableStateOf<List<AudioHomeArtist>>(listOf())

    suspend fun loadAsync(audioVM: AudioViewModel) {
        playlists.value = AudioQueueManager.playlists()
        val history = AudioQueueManager.recentPage(limit = 8, offset = 0)
        recentIsHistory.value = history.isNotEmpty()
        recent.value = history
        rebuildArtists(audioVM.itemsFlow.value)
    }

    fun rebuildArtists(songs: List<DAudio>) {
        // Artist tiles come from the loaded library pages.
        artists.value = songs
            .filter { it.artist.isNotBlank() }
            .groupBy { it.artist }
            .map { (name, list) -> AudioHomeArtist(name, list.size) }
            .sortedWith(compareByDescending<AudioHomeArtist> { it.songCount }.thenBy { it.name })
    }
}
