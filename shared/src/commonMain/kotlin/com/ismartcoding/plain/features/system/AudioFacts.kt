package com.ismartcoding.plain.features.system

import kotlinx.serialization.Serializable

@Serializable
internal data class AudioFacts(
    val id: String,
    val title: String,
    val artist: String,
    val path: String,
    val size: Long,
    val bucketId: String,
    val durationMs: Long,
    val albumFileId: String,
    val createdAt: String,
    val updatedAt: String,
    val isFavorite: Boolean,
)
