package com.ismartcoding.plain.platform

import com.ismartcoding.plain.audio.DPlaylistAudio
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.audio_notification_prompt
import kotlinx.coroutines.flow.StateFlow

interface AudioPlayer {
    val isPlayingFlow: StateFlow<Boolean>
    val progress: Long
    fun seekTo(progress: Long)
    fun pause()
    fun play()
    fun restartIfPlaying()
    fun playFromPath(path: String)
    fun justPlay(audio: DPlaylistAudio)
    fun clear()
    fun skipToPrevious()
    fun skipToNext()
    fun setPlaybackSpeed(speed: Float)
}

expect fun createAudioPlayer(): AudioPlayer

private val audioPlayer: AudioPlayer by lazy { createAudioPlayer() }

fun audioIsPlayingFlow(): StateFlow<Boolean> = audioPlayer.isPlayingFlow

fun audioPlayerProgress(): Long = audioPlayer.progress

fun audioSeekTo(progress: Long) = audioPlayer.seekTo(progress)

fun audioPause() = audioPlayer.pause()

fun audioPlay() = audioPlayer.play()

fun restartAudioIfPlaying() = audioPlayer.restartIfPlaying()

fun playAudioFromPath(path: String) = audioPlayer.playFromPath(path)

fun playAudioWithNotificationCheck(path: String) {
    checkNotificationPermission(Res.string.audio_notification_prompt) {
        audioPlayer.playFromPath(path)
    }
}

fun audioJustPlay(audio: DPlaylistAudio) = audioPlayer.justPlay(audio)

fun audioJustPlayWithNotificationCheck(audio: DPlaylistAudio) {
    checkNotificationPermission(Res.string.audio_notification_prompt) {
        audioPlayer.justPlay(audio)
    }
}

fun audioClear() = audioPlayer.clear()

fun audioSkipToPrevious() = audioPlayer.skipToPrevious()

fun audioSkipToNext() = audioPlayer.skipToNext()

fun audioSetPlaybackSpeed(speed: Float) = audioPlayer.setPlaybackSpeed(speed)
