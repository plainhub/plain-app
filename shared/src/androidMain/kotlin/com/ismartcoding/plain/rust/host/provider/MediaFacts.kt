package com.ismartcoding.plain.rust.host.provider

import kotlinx.serialization.Serializable

@Serializable
internal data class MediaFacts(
    val id: String,
    val title: String,
    val path: String,
    val size: Long,
    val bucketId: String,
    val createdAt: String,
    val updatedAt: String,
    val durationMs: Long,
    val takenAt: String?,
    val isFavorite: Boolean,
    val width: Int = 0, val height: Int = 0, val rotation: Int = 0,
)

@Serializable
internal data class AudioFacts(
    val id: String,
    val title: String,
    val artist: String,
    val path: String,
    val size: Long,
    val bucketId: String,
    val durationMs: Long,
    val albumArtUri: String,
    val albumId: String,
    val createdAt: String,
    val updatedAt: String,
    val isFavorite: Boolean,
)

@Serializable
data class MediaBucketItemFacts(
    val id: String,
    val name: String,
    val size: Long,
    val path: String,
    val updatedAt: String,
)
