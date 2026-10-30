package com.ismartcoding.plain.ui.audio

import com.ismartcoding.plain.rust.graphql.ContentSource
import com.ismartcoding.plain.ui.state.ISearchableViewModel

import com.ismartcoding.plain.models.Audio as AudioTrack
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.platform.system.DataType
import com.ismartcoding.plain.helpers.SearchHelper
import com.ismartcoding.plain.lib.withIO

/**
 * Picker state for adding library tracks to a playlist: search input plus the
 * selection sets. Follows the app's [ISearchableViewModel] search-bar pattern.
 */
class PlaylistAddItemsViewModel internal constructor(val source: ContentSource = ContentSource.Local) : ViewModel(), ISearchableViewModel<AudioTrack> {
    override val showSearchBar = mutableStateOf(false)
    override val searchActive = mutableStateOf(false)
    override val queryText = mutableStateOf("")

    val items = mutableStateOf<List<AudioTrack>>(listOf())

    /** Paths already in the playlist; unchecking one removes it on confirm. */
    val existing = mutableStateOf<Set<String>>(emptySet())
    val selected = mutableStateOf<Set<String>>(emptySet())

    suspend fun loadAsync(playlistId: String) {
        val paths = withIO { source.playlists.playlistItemsPage(playlistId, 0, 5000).map { it.path } }
        existing.value = paths.toSet()
        selected.value = paths.toSet()
        searchAsync()
    }

    suspend fun searchAsync() {
        items.value = withIO {
            source.catalog.rows(DataType.AUDIO, SearchHelper.buildTextFilter(queryText.value), 500, 0, Prefs.audioSortBy.value)
        }.filterIsInstance<AudioTrack>()
    }
}
