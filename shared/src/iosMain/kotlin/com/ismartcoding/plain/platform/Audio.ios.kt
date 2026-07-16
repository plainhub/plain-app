package com.ismartcoding.plain.platform

import androidx.compose.ui.graphics.ImageBitmap
import com.ismartcoding.plain.audio.DAudio
import com.ismartcoding.plain.audio.DPlaylistAudio
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private val stubIsPlayingFlow = MutableStateFlow(false)

actual fun audioIsPlayingFlow(): StateFlow<Boolean> = stubIsPlayingFlow

actual fun audioPlayerProgress(): Long = 0L

actual fun audioSeekTo(progress: Long) {}

actual fun audioPause() {}

actual fun audioPlay() {}

actual fun restartAudioIfPlaying() {}

actual fun playAudioFromPath(path: String) {}

actual fun playAudioWithNotificationCheck(path: String) {}

actual suspend fun getAudioMetadata(path: String): Pair<String, String> = "" to ""

actual fun playlistAudioFromPath(path: String): DPlaylistAudio {
    val name = path.substringAfterLast("/").substringBeforeLast(".")
    return DPlaylistAudio(title = name, path = path, artist = "", duration = 0L)
}

actual fun audioJustPlay(audio: DPlaylistAudio) {}

actual fun audioJustPlayWithNotificationCheck(audio: DPlaylistAudio) {}

actual fun audioClear() {}

actual fun audioSkipToPrevious() {}

actual fun audioSkipToNext() {}

actual fun loadAudioCoverBitmap(path: String): ImageBitmap? = null

actual fun getAudioAlbumArtFileId(audio: DAudio): String = ""
