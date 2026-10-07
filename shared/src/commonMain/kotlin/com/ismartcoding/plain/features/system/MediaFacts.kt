package com.ismartcoding.plain.features.system

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
)
