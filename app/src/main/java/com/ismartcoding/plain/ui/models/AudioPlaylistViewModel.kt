package com.ismartcoding.plain.ui.models

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.plain.data.DAudio
import com.ismartcoding.plain.data.DPlaylistAudio
import com.ismartcoding.plain.features.AudioPlayer
import com.ismartcoding.plain.features.ClearAudioPlaylistEvent
import com.ismartcoding.plain.preference.AudioPlayingPreference
import com.ismartcoding.plain.preference.AudioPlaylistPreference

class AudioPlaylistViewModel : ViewModel() {
    val playlistItems = mutableStateOf<List<DPlaylistAudio>>(listOf())
    val selectedPath = mutableStateOf("")

    suspend fun loadAsync(context: Context) {
        selectedPath.value = AudioPlayingPreference.getValueAsync(context)
        playlistItems.value = AudioPlaylistPreference.getValueAsync(context)
    }

    fun isInPlaylist(path: String): Boolean {
        return playlistItems.value.any { it.path == path }
    }

    suspend fun addAsync(context: Context, items: List<DAudio>) {
        val audio = items.map { it.toPlaylistAudio() }
        playlistItems.value = AudioPlaylistPreference.addAsync(context, audio)
        if (selectedPath.value.isEmpty()) {
            setCurrentPlaying(context, audio.first().path)
        }
    }

    suspend fun clearAsync(context: Context) {
        AudioPlaylistPreference.putAsync(context, listOf())
        playlistItems.value = listOf()
        AudioPlayer.clear()
        setCurrentPlaying(context, "")
        sendEvent(ClearAudioPlaylistEvent())
    }

    private suspend fun setCurrentPlaying(context: Context, path: String) {
        AudioPlayingPreference.putAsync(context, path)
        selectedPath.value = path
    }

    suspend fun playAsync(context: Context, item: DAudio) {
        val audio = item.toPlaylistAudio()
        playlistItems.value = AudioPlaylistPreference.addAsync(context, listOf(audio))
        AudioPlayer.justPlay(context, audio)
        setCurrentPlaying(context, audio.path)
    }

    suspend fun removeAsync(context: Context, path: String) {
        val newList = AudioPlaylistPreference.deleteAsync(context, setOf(path))
        playlistItems.value = newList
        if (path == selectedPath.value) {
            // If removing currently playing item
            if (newList.isNotEmpty()) {
                // Play next item if available
                val nextItem = newList[0]
                AudioPlayingPreference.putAsync(context, nextItem.path)
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
        AudioPlaylistPreference.putAsync(context, newList)
    }
}