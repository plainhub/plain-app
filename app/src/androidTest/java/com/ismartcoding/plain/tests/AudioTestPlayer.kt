package com.ismartcoding.plain.tests

import com.ismartcoding.plain.audio.DPlaylistAudio
import com.ismartcoding.plain.platform.AudioPlayer
import kotlinx.coroutines.flow.MutableStateFlow

internal class AudioTestPlayer : AudioPlayer {
    override val isPlayingFlow = MutableStateFlow(false)
    override var currentPath = ""
    override var progress = 0L
    var loads = 0
    var fail = false
    private fun checkAvailable() { check(!fail) { "Synthetic player unavailable" } }
    override suspend fun load(audio: DPlaylistAudio, positionMs: Long, speed: Float, revision: Long) {
        checkAvailable()
        loads++
        currentPath = audio.path
        progress = positionMs
        isPlayingFlow.value = true
    }
    override fun seekTo(positionMs: Long) { checkAvailable(); progress = positionMs }
    override fun pause() { checkAvailable(); isPlayingFlow.value = false }
    override fun play() { checkAvailable(); isPlayingFlow.value = true }
    override fun restartIfPlaying() { checkAvailable() }
    override fun clear() { checkAvailable(); currentPath = ""; progress = 0; isPlayingFlow.value = false }
    override fun setPlaybackSpeed(speed: Float) { checkAvailable() }
}
