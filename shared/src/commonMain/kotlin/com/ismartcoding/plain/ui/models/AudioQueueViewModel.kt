package com.ismartcoding.plain.ui.models

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.ismartcoding.plain.audio.DAudio
import com.ismartcoding.plain.audio.DPlaylistAudio
import com.ismartcoding.plain.db.AudioPlaySource
import com.ismartcoding.plain.events.ClearAudioQueueEvent
import com.ismartcoding.plain.features.audio.AudioQueueManager
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.platform.audioClear
import com.ismartcoding.plain.platform.audioJustPlay
import com.ismartcoding.plain.preferences.AudioPlayingPreference

class AudioQueueViewModel : ViewModel(), AudioQueueViewModelBase {
    val queueItems = mutableStateOf<List<DPlaylistAudio>>(listOf())
    val queueCount = mutableStateOf(0)
    val noMore = mutableStateOf(false)

    /** Drag & drop reorder only applies to the manual queue (no playback source). */
    val canReorder = mutableStateOf(false)

    /** Paths currently in the manual queue — "remove" applies to these only. */
    val queuedPaths = mutableStateOf<Set<String>>(emptySet())

    /** Playlist id of the active playback source, null when none. */
    val activePlaylistId = mutableStateOf<String?>(null)
    override val selectedPath = mutableStateOf("")

    private val pageLimit = 200

    suspend fun loadAsync() {
        selectedPath.value = AudioPlayingPreference.getValueAsync()
        refreshWindow()
    }

    fun isInQueue(path: String): Boolean = path in queuedPaths.value

    suspend fun moreAsync() {
        if (noMore.value) return
        // Refetch the whole window instead of offset-appending: manual queue
        // mutations shift ranks, so an offset append can overlap the old window
        // and duplicate tracks.
        refreshWindow(queueItems.value.size + pageLimit)
    }

    suspend fun addAsync(items: List<DAudio>) {
        val audio = items.map { it.toPlaylistAudio() }
        AudioQueueManager.enqueue(audio)
        if (selectedPath.value.isEmpty()) {
            setCurrentPlaying(audio.first().path)
        }
        refreshWindow()
    }

    /** Replace the whole queue with a single track ("open with player" flows). */
    suspend fun playSingleAsync(audio: DPlaylistAudio) {
        AudioQueueManager.clearQueue()
        AudioQueueManager.enqueue(listOf(audio))
        selectedPath.value = audio.path
        refreshWindow()
    }

    suspend fun clearAsync() {
        AudioQueueManager.clearQueue()
        AudioPlayingPreference.putAsync("")
        audioClear()
        setCurrentPlaying("")
        refreshWindow()
        sendEvent(ClearAudioQueueEvent())
    }

    /** Sync UI state after playback was started from a queue source (engine is already playing). */
    suspend fun onStarted(audio: DPlaylistAudio) {
        setCurrentPlaying(audio.path)
        refreshWindow()
    }

    private suspend fun setCurrentPlaying(path: String) {
        AudioPlayingPreference.putAsync(path)
        selectedPath.value = path
    }

    suspend fun playAsync(item: DAudio) {
        val audio = item.toPlaylistAudio()
        AudioQueueManager.enqueue(listOf(audio))
        audioJustPlay(audio)
        setCurrentPlaying(audio.path)
        refreshWindow()
    }

    suspend fun removeAsync(path: String) {
        AudioQueueManager.removeQueued(path)
        if (path == selectedPath.value) {
            val nextItem = AudioQueueManager.resolveNext(isNext = true, shuffle = false)
            if (nextItem != null) {
                AudioPlayingPreference.putAsync(nextItem.path)
                audioJustPlay(nextItem)
                selectedPath.value = nextItem.path
            }
        }
        if (AudioQueueManager.queueTotal() == 0) {
            setCurrentPlaying("")
            audioClear()
            sendEvent(ClearAudioQueueEvent())
        }
        refreshWindow()
    }

    suspend fun reorder(from: Int, to: Int) {
        if (!canReorder.value) return
        AudioQueueManager.moveQueued(from, to)
        val list = queueItems.value.toMutableList()
        if (from in list.indices && to in list.indices) {
            list.add(to, list.removeAt(from))
            queueItems.value = list
        }
    }

    private suspend fun refreshWindow(target: Int = pageLimit) {
        val total = AudioQueueManager.queueTotal()
        val window = AudioQueueManager.queuePage(0, maxOf(target, pageLimit))
        queueItems.value = window
        queueCount.value = total
        noMore.value = window.size >= total
        canReorder.value = AudioQueueManager.source().source == AudioPlaySource.NONE
        activePlaylistId.value = AudioQueueManager.activePlaylistId()
        queuedPaths.value = AudioQueueManager.queuedPaths()
    }
}
