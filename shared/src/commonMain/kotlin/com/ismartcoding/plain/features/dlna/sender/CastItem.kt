package com.ismartcoding.plain.features.dlna.sender

import com.ismartcoding.plain.models.Audio as AudioTrack
import com.ismartcoding.plain.IMedia
import com.ismartcoding.plain.models.DlnaCastType
import kotlinx.serialization.Serializable

@Serializable
data class CastItem(
    override val path: String, override val title: String, val albumArt: String = "", val artist: String = "",
    override val durationMs: Long = 0, val type: DlnaCastType,
) : IMedia {
    companion object {
        fun from(item: IMedia): CastItem = if (item is CastItem) item else if (item is AudioTrack) CastItem(item.path, item.title,
            "content://media/external/audio/albumart/${item.albumId}", item.artist, item.durationMs, DlnaCastType.AUDIO)
        else CastItem(item.path, item.title, durationMs = item.durationMs, type = DlnaCastType.VIDEO)
    }
}
