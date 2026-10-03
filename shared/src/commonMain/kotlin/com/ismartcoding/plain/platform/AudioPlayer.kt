package com.ismartcoding.plain.platform

import com.ismartcoding.plain.features.audio.AudioCommands
import com.ismartcoding.plain.audio.DPlaylistAudio
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.audio_notification_prompt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

interface AudioPlayer {
    val isPlayingFlow: StateFlow<Boolean>
    val currentPath: String
    suspend fun load(audio: DPlaylistAudio, positionMs: Long, speed: Float, revision: Long)
    val progress: Long
    fun seekTo(positionMs: Long)
    fun pause()
    fun play()
    fun restartIfPlaying()
    fun clear()
    fun setPlaybackSpeed(speed: Float)
}

expect fun createAudioPlayer(): AudioPlayer

internal val audioPlayer: AudioPlayer by lazy { createAudioPlayer() }

fun audioIsPlayingFlow(): StateFlow<Boolean> = audioPlayer.isPlayingFlow

fun audioPlayerProgress(): Long = audioPlayer.progress

/** Suspending read for non-main threads (e.g. the HTTP server): the Android player is a Media3 MediaController which only allows calls on the app thread. */
suspend fun audioPlayerProgressAsync(): Long = withContext(Dispatchers.Main) { audioPlayer.progress }

fun audioSeekTo(positionMs: Long) = AudioCommands.submit("SEEK", positionMs = positionMs)

fun audioPause() = AudioCommands.submit("PAUSE")

fun audioPlay() = AudioCommands.submit("PLAY")

fun restartAudioIfPlaying() = AudioCommands.submit("RESTART")

fun playAudioFromPath(path: String) = AudioCommands.playPath(path)

fun playAudioWithNotificationCheck(path: String) {
    checkNotificationPermission(Res.string.audio_notification_prompt) {
        AudioCommands.playPath(path)
    }
}

fun audioJustPlay(audio: DPlaylistAudio) = AudioCommands.playTrack(audio)

fun audioJustPlayWithNotificationCheck(audio: DPlaylistAudio) {
    checkNotificationPermission(Res.string.audio_notification_prompt) {
        AudioCommands.playTrack(audio)
    }
}

fun audioClear() = AudioCommands.submit("CLEAR")

fun audioSkipToPrevious() = AudioCommands.submit("PREVIOUS")

fun audioSkipToNext() = AudioCommands.submit("NEXT")

fun audioSetPlaybackSpeed(speed: Float) = AudioCommands.submit("SET_SPEED", speed = speed)
