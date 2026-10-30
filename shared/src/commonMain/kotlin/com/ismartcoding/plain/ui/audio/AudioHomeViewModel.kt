package com.ismartcoding.plain.ui.audio

import com.ismartcoding.plain.rust.graphql.ContentSource
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.ismartcoding.plain.models.Audio
import com.ismartcoding.plain.models.AudioArtist
import com.ismartcoding.plain.models.AudioPlaylist
import com.ismartcoding.plain.models.AudioAlbumCover
import com.ismartcoding.plain.ui.tags.TagsViewModel

class AudioHomeViewModel internal constructor(val source: ContentSource = ContentSource.Local) : ViewModel() {
    companion object {
        private var active: Pair<AudioHomeViewModel, TagsViewModel>? = null
        fun attach(viewModel: AudioHomeViewModel, tags: TagsViewModel) { active = viewModel to tags }
        fun detach(viewModel: AudioHomeViewModel) {
            if (active?.first === viewModel) active = null
        }
        suspend fun refreshActive() { active?.let { (viewModel, tags) -> viewModel.refreshOverview(tags) } }
    }

    val playlists = mutableStateOf<List<Pair<AudioPlaylist, Int>>>(emptyList())
    val playlistCovers = mutableStateOf<Map<String, List<AudioAlbumCover>>>(emptyMap())
    val recentItems = mutableStateOf<List<Audio>>(emptyList())
    val artists = mutableStateOf<List<AudioArtist>>(emptyList())

    suspend fun loadAsync(tagsVM: TagsViewModel) {
        playlists.value = source.playlists.playlists()
        refreshOverview(tagsVM)
        playlistCovers.value = source.playlists.covers(playlists.value.filter { it.second > 0 }.map { it.first.id })
    }

    suspend fun refreshOverview(tagsVM: TagsViewModel) {
        val overview = source.audioOverview.read()
        tagsVM.refreshRelations(overview.recentItems.map { it.id }.toSet())
        recentItems.value = overview.recentItems
        artists.value = overview.artists
    }
}
