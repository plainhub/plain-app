package com.ismartcoding.plain.platform

import androidx.compose.ui.graphics.ImageBitmap
import com.ismartcoding.plain.audio.DAudio
import com.ismartcoding.plain.audio.DPlaylistAudio

expect suspend fun getAudioMetadata(path: String): Pair<String, String>

expect fun playlistAudioFromPath(path: String): DPlaylistAudio

expect fun loadAudioCoverBitmap(path: String): ImageBitmap?

expect fun getAudioAlbumArtFileId(audio: DAudio): String
