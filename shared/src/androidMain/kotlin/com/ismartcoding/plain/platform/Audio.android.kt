package com.ismartcoding.plain.platform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.audio.AudioPlayer
import com.ismartcoding.plain.audio.DAudio
import com.ismartcoding.plain.audio.DPlaylistAudio
import com.ismartcoding.plain.audio.fromPath
import com.ismartcoding.plain.audio.getAlbumUri
import com.ismartcoding.plain.features.Permissions
import com.ismartcoding.plain.helpers.FileHelper
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.audio_notification_prompt
import kotlinx.coroutines.flow.StateFlow

actual fun audioIsPlayingFlow(): StateFlow<Boolean> = AudioPlayer.isPlayingFlow

actual fun audioPlayerProgress(): Long = AudioPlayer.playerProgress

actual fun audioSeekTo(progress: Long) = AudioPlayer.seekTo(progress)

actual fun audioPause() = AudioPlayer.pause()

actual fun audioPlay() = AudioPlayer.play()

actual fun restartAudioIfPlaying() {
    if (AudioPlayer.isPlaying()) {
        AudioPlayer.pause()
        AudioPlayer.play()
    }
}

actual fun playAudioFromPath(path: String) {
    AudioPlayer.play(appContext, DPlaylistAudio.fromPath(appContext, path))
}

actual fun playAudioWithNotificationCheck(path: String) {
    Permissions.checkNotification(appContext, Res.string.audio_notification_prompt) {
        AudioPlayer.play(appContext, DPlaylistAudio.fromPath(appContext, path))
    }
}

actual suspend fun getAudioMetadata(path: String): Pair<String, String> {
    val audio = DPlaylistAudio.fromPath(appContext, path)
    return audio.title to audio.artist
}

actual fun playlistAudioFromPath(path: String): DPlaylistAudio {
    return DPlaylistAudio.fromPath(appContext, path)
}

actual fun audioJustPlay(audio: DPlaylistAudio) {
    AudioPlayer.justPlay(appContext, audio)
}

actual fun audioJustPlayWithNotificationCheck(audio: DPlaylistAudio) {
    Permissions.checkNotification(appContext, Res.string.audio_notification_prompt) {
        AudioPlayer.justPlay(appContext, audio)
    }
}

actual fun audioClear() {
    AudioPlayer.clear()
}

actual fun audioSkipToPrevious() = AudioPlayer.skipToPrevious()

actual fun audioSkipToNext() = AudioPlayer.skipToNext()

actual fun loadAudioCoverBitmap(path: String): ImageBitmap? {
    return try {
        val retriever = android.media.MediaMetadataRetriever()
        try {
            retriever.setDataSource(path)
            retriever.embeddedPicture?.let {
                android.graphics.BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap()
            }
        } finally {
            retriever.release()
        }
    } catch (e: Exception) {
        null
    }
}

actual fun getAudioAlbumArtFileId(audio: DAudio): String {
    return FileHelper.getFileId(audio.getAlbumUri().toString())
}
