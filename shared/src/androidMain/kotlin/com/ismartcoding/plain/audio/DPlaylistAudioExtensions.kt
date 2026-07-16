package com.ismartcoding.plain.audio

import android.content.Context
import android.media.MediaMetadataRetriever
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import com.ismartcoding.plain.lib.extensions.getFilenameWithoutExtensionFromPath
import com.ismartcoding.plain.lib.extensions.pathToUri
import com.ismartcoding.plain.platform.LocaleHelper
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.unknown

/**
 * Android-specific extensions on [DPlaylistAudio] kept in androidMain because they
 * depend on MediaMetadataRetriever and Media3.
 */
@OptIn(UnstableApi::class)
fun DPlaylistAudio.toMediaItem(): MediaItem {
    val mediaMetadata = MediaMetadata.Builder()
        .setTitle(title)
        .setSubtitle(artist)
        .setArtist(artist)
        .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
        .build()

    return MediaItem.Builder()
        .setUri(path.pathToUri())
        .setMediaId(path)
        .setCustomCacheKey(path)
        .setMediaMetadata(mediaMetadata)
        .build()
}

fun DPlaylistAudio.Companion.fromPath(
    context: Context,
    path: String,
): DPlaylistAudio {
    val retriever = MediaMetadataRetriever()
    var title = path.getFilenameWithoutExtensionFromPath()
    var duration = 0L
    var artist = LocaleHelper.getString(Res.string.unknown)

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
