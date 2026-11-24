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
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.job

class AudioHomeViewModel internal constructor(val source: ContentSource = ContentSource.Local) : ViewModel() {
    companion object {
        private var active: Pair<AudioHomeViewModel, TagsViewModel>? = null
        suspend fun refreshActive() { active?.let { (viewModel, tags) -> viewModel.refreshOverview(tags) } }
    }

    private enum class RecentRefresh { KEEP_ORDER, INITIAL, RELOAD }

    private var observation: Job? = null
    private val recent = mutableStateOf<List<Audio>?>(null)
    val playlists = mutableStateOf<List<Pair<AudioPlaylist, Int>>>(emptyList())
    val recentItems: List<Audio> get() = recent.value.orEmpty()
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
        val job = currentCoroutineContext().job
        observation = job
        recent.value = null
        if (source == ContentSource.Local) active = this to tagsVM
        try {
            updates.onEach { update ->
                if (update is SubscriptionUpdate.Failed) throw update.error
                try {
                    if (update == SubscriptionUpdate.Attached) {
                        loadPlaylists()
                        refreshOverview(tagsVM, RecentRefresh.INITIAL)
                    } else {
                        refreshOverview(tagsVM)
                    }
                } catch (canceled: CancellationException) {
                    throw canceled
                } catch (error: Exception) {
                    LogCat.e("Audio overview refresh", error)
                }
            }.catch { error ->
                if (error !is Exception) throw error
                LogCat.e("Audio home updates", error)
            }.collect()
            awaitCancellation()
        } finally {
            if (observation === job) {
                observation = null
                recent.value = null
                if (active?.first === this) active = null
            }
        }
    }

    internal suspend fun loadPlaylists() {
        val job = observation?.takeIf { it.isActive } ?: return
        val updated = source.playlists.playlists(includeCovers = true)
        if (observation === job && job.isActive) playlists.value = updated
    }

    internal suspend fun refresh(tagsVM: TagsViewModel) {
        val job = observation?.takeIf { it.isActive } ?: return
        loadPlaylists()
        if (observation === job && job.isActive) refreshOverview(tagsVM, RecentRefresh.RELOAD)
    }

    private suspend fun refreshOverview(tagsVM: TagsViewModel, refresh: RecentRefresh = RecentRefresh.KEEP_ORDER) {
        val job = observation?.takeIf { it.isActive } ?: return
        val previous = recent.value
        val reload = refresh == RecentRefresh.RELOAD || (refresh == RecentRefresh.INITIAL && previous == null)
        val ids = if (reload) null else previous.orEmpty().map { it.id }
        val overview = source.audioOverview.read(ids)
        if (observation !== job || !job.isActive) return
        artists.value = overview.artists
        if (!reload && previous == null) return

        val items = if (ids == null) overview.recentItems else {
            val byId = overview.recentItems.associateBy { it.id }
            ids.mapNotNull(byId::get)
        }
        tagsVM.refreshRelations(items.map { it.id }.toSet())
        if (observation === job && job.isActive && (refresh == RecentRefresh.RELOAD || recent.value === previous)) {
            recent.value = items
        }
    }
}
