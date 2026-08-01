package com.ismartcoding.plain.platform

import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.audio.AudioPlayer as AndroidAudioPlayer
import com.ismartcoding.plain.audio.DPlaylistAudio
import com.ismartcoding.plain.audio.fromPath
import kotlinx.coroutines.flow.StateFlow

actual fun createAudioPlayer(): AudioPlayer = ExoPlayerAudioPlayer

private object ExoPlayerAudioPlayer : AudioPlayer {
    override val isPlayingFlow: StateFlow<Boolean> = AndroidAudioPlayer.isPlayingFlow

    override val progress: Long
        get() = AndroidAudioPlayer.playerProgress

    override fun seekTo(progress: Long) = AndroidAudioPlayer.seekTo(progress)

    override fun pause() = AndroidAudioPlayer.pause()

    override fun play() = AndroidAudioPlayer.play()

    override fun restartIfPlaying() {
        if (AndroidAudioPlayer.isPlaying()) {
            AndroidAudioPlayer.pause()
            AndroidAudioPlayer.play()
        }
    }

    override fun playFromPath(path: String) {
        AndroidAudioPlayer.play(appContext, DPlaylistAudio.fromPath(appContext, path))
    }

    override fun justPlay(audio: DPlaylistAudio) {
        AndroidAudioPlayer.justPlay(appContext, audio)
    }

    override fun clear() = AndroidAudioPlayer.clear()

    override fun skipToPrevious() = AndroidAudioPlayer.skipToPrevious()

    override fun skipToNext() = AndroidAudioPlayer.skipToNext()

    override fun setPlaybackSpeed(speed: Float) = AndroidAudioPlayer.setPlaybackSpeed(speed)
}
