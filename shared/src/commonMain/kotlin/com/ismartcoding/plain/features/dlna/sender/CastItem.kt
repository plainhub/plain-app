package com.ismartcoding.plain.features.dlna.sender

import com.ismartcoding.plain.db.IMedia
import com.ismartcoding.plain.audio.DAudio
import kotlinx.serialization.Serializable

@Serializable
data class CastItem(
    override val path: String, override val title: String, val albumArt: String = "", val artist: String = "",
    override val durationMs: Long = 0, val audio: Boolean = false,
) : IMedia {
    companion object {
        fun from(item: IMedia): CastItem = if (item is CastItem) item else if (item is DAudio) CastItem(item.path, item.title,
            "content://media/external/audio/albumart/${item.albumId}", item.artist, item.durationMs, true)
        else CastItem(item.path, item.title, durationMs = item.durationMs)
    }
}
