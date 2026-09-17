package com.ismartcoding.plain.ui.models

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.ismartcoding.plain.audio.DAudio
import com.ismartcoding.plain.db.IData
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.audio.AudioQueueManager
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.platform.searchMedia
import com.ismartcoding.plain.preferences.AudioSortByPreference

/**
 * Picker state for adding library tracks to a playlist: search input plus the
 * selection sets. Follows the app's [ISearchableViewModel] search-bar pattern.
 */
class PlaylistAddSongsViewModel : ViewModel(), ISearchableViewModel<DAudio> {
    override val showSearchBar = mutableStateOf(false)
    override val searchActive = mutableStateOf(false)
    override val queryText = mutableStateOf("")

    val items = mutableStateOf<List<DAudio>>(listOf())

    /** Paths already in the playlist; unchecking one removes it on confirm. */
    val existing = mutableStateOf<Set<String>>(emptySet())
    val selected = mutableStateOf<Set<String>>(emptySet())

    suspend fun loadAsync(playlistId: String) {
        val paths = withIO { AudioQueueManager.playlistSongsPage(playlistId, 0, 5000).map { it.audioPath } }
        existing.value = paths.toSet()
        selected.value = paths.toSet()
        searchAsync()
    }

    suspend fun searchAsync() {
        items.value = withIO {
            searchMedia(DataType.AUDIO, queryText.value, 500, 0, AudioSortByPreference.getValueAsync())
        }.filterIsInstance<DAudio>()
    }
}
