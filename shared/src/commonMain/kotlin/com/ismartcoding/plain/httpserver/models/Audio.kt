package com.ismartcoding.plain.httpserver.models

import kotlin.time.Instant

data class Audio(
    override val id: ID,
    override val title: String,
    val artist: String,
    override val path: String,
    val durationMs: Long,
    override val size: Long,
    override val bucketId: ID,
    val albumFileId: String,
    override val createdAt: Instant,
    override val updatedAt: Instant,
    val isFavorite: Boolean,
) : MediaItem

data class AudioItem(
    val title: String,
    val artist: String,
    val path: String,
    val durationMs: Long,
)

data class AudioPlayHistory(
    val path: String,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val playCount: Int,
    val playedAt: Instant,
)
