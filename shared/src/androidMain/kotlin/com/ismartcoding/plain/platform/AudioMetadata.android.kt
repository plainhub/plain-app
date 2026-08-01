package com.ismartcoding.plain.platform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.audio.DAudio
import com.ismartcoding.plain.audio.DPlaylistAudio
import com.ismartcoding.plain.audio.fromPath
import com.ismartcoding.plain.audio.getAlbumUri
import com.ismartcoding.plain.helpers.FileHelper
import com.ismartcoding.plain.helpers.withIO

actual suspend fun getAudioMetadata(path: String): Pair<String, String> {
    val audio = DPlaylistAudio.fromPath(appContext, path)
    return audio.title to audio.artist
}

actual fun playlistAudioFromPath(path: String): DPlaylistAudio {
    return DPlaylistAudio.fromPath(appContext, path)
}

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
