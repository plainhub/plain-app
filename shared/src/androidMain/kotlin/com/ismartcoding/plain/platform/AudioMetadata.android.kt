package com.ismartcoding.plain.platform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.audio.DAudio
import com.ismartcoding.plain.audio.DPlaylistAudio
import com.ismartcoding.plain.audio.fromPath
import com.ismartcoding.plain.audio.getAlbumUri
import com.ismartcoding.plain.helpers.getFileId
import com.ismartcoding.plain.lib.EmbeddedLyrics
import com.ismartcoding.plain.lib.extensions.pathToUri

actual suspend fun getAudioMetadata(path: String): Pair<String, String> {
    val audio = DPlaylistAudio.fromPath(appContext, path)
    return audio.title to audio.artist
}

actual suspend fun getAudioLyrics(path: String): String {
    val stream =
        if (path.startsWith("/")) {
            runCatching { java.io.FileInputStream(path) }.getOrNull() ?: return ""
        } else {
            runCatching { appContext.contentResolver.openInputStream(path.pathToUri()) }.getOrNull() ?: return ""
        }
    return try {
        EmbeddedLyrics.extract(
            object : EmbeddedLyrics.Reader() {
                override fun read(buffer: ByteArray, offset: Int, length: Int): Int = stream.read(buffer, offset, length)

                override fun close() {
                    stream.close()
                }
            },
        )
    } catch (e: Exception) {
        ""
    } finally {
        runCatching { stream.close() }
    }
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
    return getFileId(audio.getAlbumUri().toString())
}
