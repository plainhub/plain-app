package com.ismartcoding.plain.web.models

import kotlin.time.Instant

data class Audio(
    val id: ID,
    val title: String,
    val artist: String,
    val path: String,
    val duration: Long,
    val size: Long,
    val bucketId: String,
    val albumFileId: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val isFavorite: Boolean,
)

data class PlaylistAudio(
    val title: String,
    val artist: String,
    val path: String,
    val duration: Long,
)
