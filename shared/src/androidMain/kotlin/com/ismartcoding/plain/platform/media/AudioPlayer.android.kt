package com.ismartcoding.plain.platform.media


import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.audio_notification_prompt
import com.ismartcoding.plain.ui.permissions.checkNotificationPermission
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.models.AudioItem
import kotlinx.coroutines.flow.StateFlow

actual fun checkAudioPlaybackPermission(onGranted: () -> Unit) =
    checkNotificationPermission(Res.string.audio_notification_prompt, onGranted)

actual fun createAudioPlayer(): AudioPlayer = ExoPlayerAudioPlayer

private object ExoPlayerAudioPlayer : AudioPlayer {
    override val isPlayingFlow: StateFlow<Boolean> = AndroidAudioPlayer.isPlayingFlow

    override val currentPath: String get() = AndroidAudioPlayer.currentPath

    override fun bindRevision(revision: Long) = AndroidAudioPlayer.bindRevision(revision)


    override suspend fun load(audio: AudioItem, positionMs: Long, speed: Float, revision: Long) = AndroidAudioPlayer.load(appContext, audio, positionMs, speed, revision)

    override val progress: Long
        get() = AndroidAudioPlayer.playerProgress

    override suspend fun seekTo(positionMs: Long) = AndroidAudioPlayer.seekTo(positionMs)

    override suspend fun pause() = AndroidAudioPlayer.pause()

    override suspend fun play() = AndroidAudioPlayer.play()

    override suspend fun restartIfPlaying() {
        if (AndroidAudioPlayer.isPlaying()) {
            AndroidAudioPlayer.pause()
            AndroidAudioPlayer.play()
        }
    }

    override suspend fun clear() = AndroidAudioPlayer.clear()

    override suspend fun setPlaybackSpeed(speed: Float) = AndroidAudioPlayer.setPlaybackSpeed(speed)
}
