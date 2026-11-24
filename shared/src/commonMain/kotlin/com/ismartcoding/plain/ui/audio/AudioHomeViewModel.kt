package com.ismartcoding.plain.ui.audio

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.models.Audio
import com.ismartcoding.plain.models.AudioArtist
import com.ismartcoding.plain.models.AudioPlaylist
import com.ismartcoding.plain.models.FileIndexScanState
import com.ismartcoding.plain.models.MediaChange
import com.ismartcoding.plain.models.MediaDataType
import com.ismartcoding.plain.rust.graphql.AppSubscriptions
import com.ismartcoding.plain.rust.graphql.ContentSource
import com.ismartcoding.plain.rust.graphql.GraphqlOperation
import com.ismartcoding.plain.rust.graphql.SubscriptionUpdate
import com.ismartcoding.plain.ui.tags.TagsViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.merge

class AudioHomeViewModel internal constructor(val source: ContentSource = ContentSource.Local) : ViewModel() {
    companion object {
        private var active: Pair<AudioHomeViewModel, TagsViewModel>? = null
        fun attach(viewModel: AudioHomeViewModel, tags: TagsViewModel) {
            if (viewModel.source == ContentSource.Local) active = viewModel to tags
        }
        fun detach(viewModel: AudioHomeViewModel) {
            if (active?.first === viewModel) active = null
        }
        suspend fun refreshActive() { active?.let { (viewModel, tags) -> viewModel.refreshOverview(tags) } }
    }

    val playlists = mutableStateOf<List<Pair<AudioPlaylist, Int>>>(emptyList())
    val recentItems = mutableStateOf<List<Audio>>(emptyList())
    val artists = mutableStateOf<List<AudioArtist>>(emptyList())

    internal suspend fun observe(tagsVM: TagsViewModel) {
        val updates: Flow<SubscriptionUpdate<*>> = when (source) {
            ContentSource.Local -> merge(
                AppSubscriptions.subscription<Boolean>("contentChanged"),
                AppSubscriptions.mediaUpdated.filter {
                    it is SubscriptionUpdate.Data && MediaDataType.AUDIO in it.value.types
                },
                AppSubscriptions.fileIndexScanProgressUpdated.filter {
                    it is SubscriptionUpdate.Data && it.value.state != FileIndexScanState.RUNNING
                },
            )
            is ContentSource.Remote -> source.client.subscriptionUpdates(listOf(
                GraphqlOperation("subscription { contentChanged }"),
                GraphqlOperation("subscription { mediaUpdated { types } }"),
            )).filter { update ->
                if (update !is SubscriptionUpdate.Data) return@filter true
                val data = checkNotNull(update.value.data)
                "contentChanged" in data || data["mediaUpdated"]?.let {
                    MediaDataType.AUDIO in JsonHelper.jsonDecodeFromElement<MediaChange>(it).types
                } == true
            }
        }
        updates.collect { update ->
            if (update is SubscriptionUpdate.Failed) throw update.error
            try {
                if (update == SubscriptionUpdate.Attached) loadAsync(tagsVM)
                else refreshOverview(tagsVM)
            }
            catch (canceled: CancellationException) { throw canceled }
            catch (error: Exception) { LogCat.e("Audio overview refresh", error) }
        }
    }

    suspend fun loadAsync(tagsVM: TagsViewModel) {
        playlists.value = source.playlists.playlists(includeCovers = true)
        refreshOverview(tagsVM)
    }

    suspend fun refreshOverview(tagsVM: TagsViewModel) {
        val overview = source.audioOverview.read()
        tagsVM.refreshRelations(overview.recentItems.map { it.id }.toSet())
        recentItems.value = overview.recentItems
        artists.value = overview.artists
    }
}
