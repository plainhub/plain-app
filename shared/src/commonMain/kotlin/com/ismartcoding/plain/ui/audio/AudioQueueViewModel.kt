package com.ismartcoding.plain.ui.audio

import com.ismartcoding.plain.rust.graphql.AppSubscriptions
import com.ismartcoding.plain.rust.graphql.changes
import com.ismartcoding.plain.features.files.FileSortBy
import com.ismartcoding.plain.features.audio.toPlaylistAudio
import com.ismartcoding.plain.models.Audio as AudioTrack
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModel
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.features.audio.AudioCommands
import com.ismartcoding.plain.features.audio.AudioQueueApi
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.audio_notification_prompt
import com.ismartcoding.plain.rust.graphql.ContentSource
import com.ismartcoding.plain.ui.permissions.checkNotificationPermission
import com.ismartcoding.plain.ui.state.launchSafe
import kotlinx.coroutines.flow.Flow
import com.ismartcoding.plain.models.AudioItem
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import com.ismartcoding.plain.lib.logcat.LogCat

class AudioQueueViewModel internal constructor(
    val source: ContentSource = ContentSource.Local,
    private val queue: AudioQueueApi = source.queue,
    private val checkPlaybackPermission: (() -> Unit) -> Unit = { onGranted ->
        checkNotificationPermission(Res.string.audio_notification_prompt, onGranted)
    },
    private val startPlayback: suspend (AudioItem) -> Unit = { AudioCommands.play(it, source) },
    sourceChanges: Flow<Unit>? = null,
) : ViewModel() {
    companion object {
        private val playerVisible = MutableStateFlow(false)
        private val active = mutableSetOf<AudioQueueViewModel>()
        suspend fun refreshActive() {
            for (viewModel in active.toList()) viewModel.loadAsync()
        }
    }

    val audioPlayerVisible = playerVisible
    val playMode = mutableStateOf(com.ismartcoding.plain.features.audio.MediaPlayMode.REPEAT)
    val playbackSpeed = androidx.compose.runtime.mutableFloatStateOf(1f)
    val queueItems = mutableStateOf<List<AudioItem>>(listOf())
    val queueCount = mutableStateOf(0)
    val noMore = mutableStateOf(false)

    val error = mutableStateOf<String?>(null)
    val canReorder = mutableStateOf(false)

    val queuedMediaIds = mutableStateOf<Set<String>>(emptySet())

    val selectedMediaId = mutableStateOf<String?>(null)
    val selectedPath = mutableStateOf("")

    private val pageLimit = 200

    init {
        active.add(this)
        viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                val changes = sourceChanges ?: if (source is com.ismartcoding.plain.rust.graphql.ContentSource.Local) AppSubscriptions.contentChanged.changes()
                    else kotlinx.coroutines.flow.merge(
                        (source as com.ismartcoding.plain.rust.graphql.ContentSource.Remote).client.changes(listOf(com.ismartcoding.plain.rust.graphql.GraphqlOperation("subscription { contentChanged }"), com.ismartcoding.plain.rust.graphql.GraphqlOperation("subscription { permissionsUpdated }"))),
                        com.ismartcoding.plain.features.audio.AudioCommands.changed.filter { com.ismartcoding.plain.features.audio.AudioCommands.output.value.source.key == source.key },
                    )
                changes.collect {
                    try { loadAsync() }
                    catch (cancelled: CancellationException) { throw cancelled }
                    catch (error: Exception) { LogCat.e("Refresh audio queue", error) }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) {
                error.value = failure.message ?: failure.toString()
                LogCat.e("Observe audio queue", failure)
            }
        }
    }

    override fun onCleared() {
        active.remove(this)
        super.onCleared()
    }

    suspend fun loadAsync() {
        try {
            val snapshot = refreshWindow()
            val playback = snapshot.audioPlayback
            val rendered = if (source is com.ismartcoding.plain.rust.graphql.ContentSource.Remote && com.ismartcoding.plain.features.audio.AudioCommands.output.value.source.key == source.key) com.ismartcoding.plain.features.audio.AudioCommands.output.value.track else null
            selectedPath.value = rendered?.path ?: playback.path.orEmpty()
            selectedMediaId.value = rendered?.mediaId ?: playback.mediaId
            playMode.value = playback.mode
            if (source is com.ismartcoding.plain.rust.graphql.ContentSource.Remote) {
                playbackSpeed.floatValue = com.ismartcoding.plain.models.audioPlaybackSpeed(snapshot.prefs)
            }
            error.value = null
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Exception) {
            error.value = failure.message ?: failure.toString()
            throw failure
        }
    }

    fun isInQueue(mediaId: String): Boolean = mediaId in queuedMediaIds.value

    suspend fun moreAsync() {
        if (noMore.value) return
        // Refetch the whole window instead of offset-appending: manual queue
        // mutations shift ranks, so an offset append can overlap the old window
        // and duplicate tracks.
        refreshWindow(queueItems.value.size + pageLimit)
    }

    suspend fun addAsync(items: List<AudioTrack>) {
        val audio = items.map { it.toPlaylistAudio() }
        queue.enqueue(audio)
        loadAsync()
    }

    fun playQuery(query: String, sortBy: FileSortBy, shuffle: Boolean = false) {
        checkPlaybackPermission {
            viewModelScope.launchSafe {
                val start = withIO { queue.replace(query, sortBy, shuffle) }
                if (start != null) {
                    startPlayback(start)
                }
                setCurrentPlaying(start)
                refreshWindow()
            }
        }
    }

    suspend fun playSingleAsync(audio: AudioItem) {
        if (source is com.ismartcoding.plain.rust.graphql.ContentSource.Local || audio.mediaId != null) {
            queue.clearQueue()
            queue.enqueue(listOf(audio))
        }
        com.ismartcoding.plain.features.audio.AudioCommands.play(audio, source)
        selectedPath.value = audio.path
        selectedMediaId.value = audio.mediaId
        refreshWindow()
    }

    suspend fun clearAsync() {
        com.ismartcoding.plain.features.audio.AudioCommands.clearQueue(source)
        selectedPath.value = ""
        selectedMediaId.value = null
        refreshWindow()
    }

    suspend fun onStarted(audio: AudioItem) {
        setCurrentPlaying(audio)
        refreshWindow()
    }

    private fun setCurrentPlaying(audio: AudioItem?) {
        selectedPath.value = audio?.path.orEmpty()
        selectedMediaId.value = audio?.mediaId
    }

    suspend fun playAsync(item: AudioTrack) {
        val audio = item.toPlaylistAudio()
        com.ismartcoding.plain.features.audio.AudioCommands.play(audio, source)
        setCurrentPlaying(audio)
        refreshWindow()
    }

    suspend fun enqueueAndPlayAsync(item: AudioTrack) {
        val audio = item.toPlaylistAudio()
        com.ismartcoding.plain.features.audio.AudioCommands.play(audio, source, enqueue = true)
        setCurrentPlaying(audio)
        refreshWindow()
    }

    suspend fun removeAsync(mediaId: String) {
        com.ismartcoding.plain.features.audio.AudioCommands.removeQueued(source, mediaId)
        loadAsync()
    }

    suspend fun reorder(from: Int, to: Int) {
        val list = queueItems.value.toMutableList()
        if (from !in list.indices || to !in list.indices) return
        list.add(to,list.removeAt(from))
        queue.reorderQueued(list.map { requireNotNull(it.mediaId) })
        queueItems.value = list
    }

    private suspend fun refreshWindow(target: Int = pageLimit): com.ismartcoding.plain.models.AudioQueueSnapshot {
        val snapshot = queue.snapshot(maxOf(target, pageLimit))
        queueItems.value = snapshot.items
        queueCount.value = snapshot.count
        noMore.value = snapshot.items.size >= snapshot.count
        canReorder.value = true
        queuedMediaIds.value = snapshot.queuedIds.mapNotNull { it.mediaId }.toSet()
        return snapshot
    }
}
