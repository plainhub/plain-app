package com.ismartcoding.plain.ui.models

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.ismartcoding.plain.audio.DAudio
import com.ismartcoding.plain.audio.DPlaylistAudio
import com.ismartcoding.plain.audio.toPlaylistAudio
import com.ismartcoding.plain.events.ClearAudioPlaylistEvent
import com.ismartcoding.plain.lib.channel.sendEvent
import com.ismartcoding.plain.platform.audioClear
import com.ismartcoding.plain.platform.audioJustPlay
import com.ismartcoding.plain.preferences.AudioPlayingPreference
import com.ismartcoding.plain.preferences.AudioPlaylistPreference

class AudioPlaylistViewModel : ViewModel(), AudioPlaylistViewModelBase {
    val playlistItems = mutableStateOf<List<DPlaylistAudio>>(listOf())
    override val selectedPath = mutableStateOf("")

    suspend fun loadAsync() {
        selectedPath.value = AudioPlayingPreference.getValueAsync()
        playlistItems.value = AudioPlaylistPreference.getValueAsync()
    }

    fun isInPlaylist(path: String): Boolean {
        return playlistItems.value.any { it.path == path }
    }

    suspend fun addAsync(items: List<DAudio>) {
        val audio = items.map { it.toPlaylistAudio() }
        playlistItems.value = AudioPlaylistPreference.addAsync(audio)
        if (selectedPath.value.isEmpty()) {
            setCurrentPlaying(audio.first().path)
        }
    }

    suspend fun clearAsync() {
        AudioPlaylistPreference.putAsync(listOf())
        playlistItems.value = listOf()
        audioClear()
        setCurrentPlaying("")
        sendEvent(ClearAudioPlaylistEvent())
    }

    private suspend fun setCurrentPlaying(path: String) {
        AudioPlayingPreference.putAsync(path)
        selectedPath.value = path
    }

    suspend fun playAsync(item: DAudio) {
        val audio = item.toPlaylistAudio()
        playlistItems.value = AudioPlaylistPreference.addAsync(listOf(audio))
        audioJustPlay(audio)
        setCurrentPlaying(audio.path)
    }

    suspend fun removeAsync(path: String) {
        val newList = AudioPlaylistPreference.deleteAsync(setOf(path))
        playlistItems.value = newList
        if (path == selectedPath.value) {
            if (newList.isNotEmpty()) {
                val nextItem = newList[0]
                AudioPlayingPreference.putAsync(nextItem.path)
                audioJustPlay(nextItem)
            }
        }
        if (newList.isEmpty()) {
            setCurrentPlaying("")
            audioClear()
            sendEvent(ClearAudioPlaylistEvent())
        }
    }

    suspend fun reorder(from: Int, to: Int) {
        val newList = playlistItems.value.toMutableList()
        newList.apply {
            add(to, removeAt(from))
        }
        playlistItems.value = newList
        AudioPlaylistPreference.putAsync(newList)
    }
}
