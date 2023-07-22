package com.ismartcoding.plain.features.audio

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Parcelable
import com.ismartcoding.lib.extensions.getFilenameWithoutExtensionFromPath
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
            retriever.setDataSource(context, Uri.parse(path))
            var title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: ""
            if (title.isEmpty()) {
                title = path.getFilenameWithoutExtensionFromPath()
            }
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
            var artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: ""
            if (artist.isEmpty()) {
                artist = getString(R.string.unknown)
            }
            retriever.release()
            return DPlaylistAudio(title, path, artist, duration / 1000)
        }

        private const  val serialVersionUID = -11L
    }
}