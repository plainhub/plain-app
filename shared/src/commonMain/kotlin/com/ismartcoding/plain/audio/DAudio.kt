package com.ismartcoding.plain.audio

import com.ismartcoding.plain.lib.extensions.formatDurationMs
import com.ismartcoding.plain.db.IData
import com.ismartcoding.plain.data.IItemMetadata
import com.ismartcoding.plain.db.IMedia
import kotlin.time.Instant

data class DAudio(
    override var id: String,
    override val title: String,
    val artist: String,
    override val path: String,
    override val durationMs: Long,
    override val size: Long,
    val bucketId: String,
    val albumId: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val isFavorite: Boolean = false,
) : IItemMetadata, IMedia, IData {
    fun getSubtitle(): String {
        return listOf(artist, durationMs.formatDurationMs()).filter { it.isNotEmpty() }.joinToString(" · ")
    }

    fun toPlaylistAudio(): DPlaylistAudio {
        return DPlaylistAudio(title, path, artist, durationMs, albumId)
    }
}
