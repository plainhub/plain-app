package com.ismartcoding.plain.platform

import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.audio.AudioPlayer
import com.ismartcoding.plain.audio.DPlaylistAudio
import com.ismartcoding.plain.features.Permissions
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.audio_notification_prompt
import kotlinx.coroutines.flow.StateFlow

actual fun audioIsPlayingFlow(): StateFlow<Boolean> = AudioPlayer.isPlayingFlow

actual fun audioPlayerProgress(): Long = AudioPlayer.playerProgress

actual fun audioSeekTo(progress: Long) = AudioPlayer.seekTo(progress)

actual fun audioPause() = AudioPlayer.pause()

actual fun audioPlay() = AudioPlayer.play()

actual fun playAudioFromPath(path: String) {
    AudioPlayer.play(appContext, DPlaylistAudio.fromPath(appContext, path))
}

actual fun playAudioWithNotificationCheck(path: String) {
    Permissions.checkNotification(appContext, Res.string.audio_notification_prompt) {
        AudioPlayer.play(appContext, DPlaylistAudio.fromPath(appContext, path))
    }
}
