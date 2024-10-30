package com.ismartcoding.plain.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.os.Parcelable
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MediaMetadata.MEDIA_TYPE_MUSIC
import androidx.media3.common.util.UnstableApi
import com.ismartcoding.lib.extensions.formatDuration
import com.ismartcoding.lib.extensions.getFilenameWithoutExtensionFromPath
import com.ismartcoding.lib.extensions.pathToUri
import com.ismartcoding.plain.R
import com.ismartcoding.plain.features.locale.LocaleHelper.getString
import kotlinx.parcelize.Parcelize
import java.io.Serializable

@OptIn(UnstableApi::class)
@Parcelize
@kotlinx.serialization.Serializable
data class DPlaylistAudio(
    val title: String,
    val path: String,
    val artist: String,
    val duration: Long,
) : Parcelable, Serializable {

    fun toMediaItem(): MediaItem {
        return MediaItem.Builder()
            .setUri(path.pathToUri())
            .setMediaId(path)
            .setCustomCacheKey(path)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setSubtitle(artist)
                    .setArtist(artist)
                    .setMediaType(MEDIA_TYPE_MUSIC)
                    .build()
            )
            .build()
    }

    fun getSubtitle(): String {
        return listOf(artist, duration.formatDuration()).filter { it.isNotEmpty() }.joinToString(" · ")
    }

    companion object {
        fun fromPath(
            context: Context,
            path: String,
        ): DPlaylistAudio {
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
            }
            return DPlaylistAudio(title, path, artist, duration / 1000)
        }

        private const val serialVersionUID = -11L
    }
}
