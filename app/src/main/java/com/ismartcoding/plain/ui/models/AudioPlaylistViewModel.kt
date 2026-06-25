package com.ismartcoding.plain.ui.models
import com.ismartcoding.plain.preferences.*

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.ismartcoding.plain.lib.channel.sendEvent
import com.ismartcoding.plain.audio.DAudio
import com.ismartcoding.plain.audio.DPlaylistAudio
import com.ismartcoding.plain.audio.AudioPlayer
import com.ismartcoding.plain.events.ClearAudioPlaylistEvent
import com.ismartcoding.plain.preferences.AudioPlayingPreference
import com.ismartcoding.plain.preferences.AudioPlaylistPreference

class AudioPlaylistViewModel : ViewModel() {
    val playlistItems = mutableStateOf<List<DPlaylistAudio>>(listOf())
    val selectedPath = mutableStateOf("")

    suspend fun loadAsync(context: Context) {
        selectedPath.value = AudioPlayingPreference.getValueAsync()
        playlistItems.value = AudioPlaylistPreference.getValueAsync()
    }

    fun isInPlaylist(path: String): Boolean {
        return playlistItems.value.any { it.path == path }
    }

    suspend fun addAsync(context: Context, items: List<DAudio>) {
        val audio = items.map { it.toPlaylistAudio() }
        playlistItems.value = AudioPlaylistPreference.addAsync(audio)
        if (selectedPath.value.isEmpty()) {
            setCurrentPlaying(context, audio.first().path)
        }
    }

    suspend fun clearAsync(context: Context) {
        AudioPlaylistPreference.putAsync(listOf())
        playlistItems.value = listOf()
        AudioPlayer.clear()
        setCurrentPlaying(context, "")
        sendEvent(ClearAudioPlaylistEvent())
    }

    private suspend fun setCurrentPlaying(context: Context, path: String) {
        AudioPlayingPreference.putAsync(path)
        selectedPath.value = path
    }

    suspend fun playAsync(context: Context, item: DAudio) {
        val audio = item.toPlaylistAudio()
        playlistItems.value = AudioPlaylistPreference.addAsync(listOf(audio))
        AudioPlayer.justPlay(context, audio)
        setCurrentPlaying(context, audio.path)
    }

    suspend fun removeAsync(context: Context, path: String) {
        val newList = AudioPlaylistPreference.deleteAsync(setOf(path))
        playlistItems.value = newList
        if (path == selectedPath.value) {
            // If removing currently playing item
            if (newList.isNotEmpty()) {
                // Play next item if available
                val nextItem = newList[0]
                AudioPlayingPreference.putAsync(nextItem.path)
                AudioPlayer.justPlay(context, nextItem)
            }
        }
        if (newList.isEmpty()) {
            setCurrentPlaying(context, "")
            AudioPlayer.clear()
            sendEvent(ClearAudioPlaylistEvent())
        }
    }

    suspend fun reorder(context: Context, from: Int, to: Int) {
        val newList = playlistItems.value.toMutableList()
        newList.apply {
            add(to, removeAt(from))
        }
        playlistItems.value = newList
        AudioPlaylistPreference.putAsync(newList)
    }
}