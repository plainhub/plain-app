package com.ismartcoding.plain.models

import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class AudioPlaylist(
    var id: String,
    var name: String,
    var itemCount: Int,
    var createdAt: Instant,
    var updatedAt: Instant,
    val coverAlbums: List<AudioAlbumCover> = emptyList(),
)
