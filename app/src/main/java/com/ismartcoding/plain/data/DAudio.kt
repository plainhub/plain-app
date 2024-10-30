package com.ismartcoding.plain.data

import android.net.Uri
import com.ismartcoding.lib.extensions.formatDuration
import kotlinx.datetime.Instant

data class DAudio(
    override var id: String,
    val title: String,
    val artist: String,
    override val path: String,
    override val duration: Long,
    val size: Long,
    val bucketId: String,
    val albumId: String,
    val createdAt: Instant,
    val updatedAt: Instant,
) : IMedia, IData {
    fun toPlaylistAudio(): DPlaylistAudio {
        return DPlaylistAudio(title, path, artist, duration)
    }

    fun getSubtitle(): String {
        return listOf(artist, duration.formatDuration()).filter { it.isNotEmpty() }.joinToString(" · ")
    }

    fun getAlbumUri(): Uri {
        val albumArtUri = Uri.parse("content://media/external/audio/albumart")
        return Uri.withAppendedPath(albumArtUri, albumId)
    }
}
