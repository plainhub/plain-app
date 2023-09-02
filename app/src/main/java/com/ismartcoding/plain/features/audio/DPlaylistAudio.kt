package com.ismartcoding.plain.features.audio

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Parcelable
import com.ismartcoding.lib.extensions.getFilenameWithoutExtensionFromPath
import com.ismartcoding.lib.extensions.pathToUri
import com.ismartcoding.plain.R
import com.ismartcoding.plain.features.locale.LocaleHelper.getString
import kotlinx.parcelize.Parcelize
import java.io.Serializable

@Parcelize
@kotlinx.serialization.Serializable
data class DPlaylistAudio(
    val title: String,
    val path: String,
    val artist: String,
    val duration: Long
) : Parcelable, Serializable {
    companion object {
        fun fromPath(context: Context, path: String): DPlaylistAudio {
            val retriever = MediaMetadataRetriever()
            var title = path.getFilenameWithoutExtensionFromPath()
            var duration = 0L
            var artist = getString(R.string.unknown)
            try {
                retriever.setDataSource(context, path.pathToUri())
                val keyTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: ""
                if (keyTitle.isNotEmpty()) {
                    title = keyTitle
                }
                duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
                val keyArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: ""
                if (keyArtist.isNotEmpty()) {
                    artist = keyArtist
                }
                retriever.release()
            } catch (ex: Throwable) {
                ex.printStackTrace()
            } finally {
            }
            return DPlaylistAudio(title, path, artist, duration / 1000)
        }

        private const val serialVersionUID = -11L
    }
}