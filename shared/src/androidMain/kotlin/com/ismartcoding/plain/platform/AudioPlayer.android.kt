package com.ismartcoding.plain.platform

import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.audio.AudioPlayer as AndroidAudioPlayer
import com.ismartcoding.plain.audio.DPlaylistAudio
import kotlinx.coroutines.flow.StateFlow

actual fun createAudioPlayer(): AudioPlayer = ExoPlayerAudioPlayer

private object ExoPlayerAudioPlayer : AudioPlayer {
    override val isPlayingFlow: StateFlow<Boolean> = AndroidAudioPlayer.isPlayingFlow

    override val currentPath: String get() = AndroidAudioPlayer.currentPath

    override suspend fun load(audio: DPlaylistAudio, positionMs: Long, speed: Float, revision: Long) = AndroidAudioPlayer.load(appContext, audio, positionMs, speed, revision)

    override val progress: Long
        get() = AndroidAudioPlayer.playerProgress

    override fun seekTo(positionMs: Long) = AndroidAudioPlayer.seekTo(positionMs)

    override fun pause() = AndroidAudioPlayer.pause()

    override fun play() = AndroidAudioPlayer.play()

    override fun restartIfPlaying() {
        if (AndroidAudioPlayer.isPlaying()) {
            AndroidAudioPlayer.pause()
            AndroidAudioPlayer.play()
        }
    }

    override fun clear() = AndroidAudioPlayer.clear()

    override fun setPlaybackSpeed(speed: Float) = AndroidAudioPlayer.setPlaybackSpeed(speed)
}
