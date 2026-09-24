package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.data.DVideo
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLType
import kotlin.time.Instant

@GraphQLType
data class Video(
    override var id: ID,
    override var title: String,
    override var path: String,
    val durationMs: Long,
    override val size: Long,
    override val bucketId: ID,
    override val createdAt: Instant,
    override val updatedAt: Instant,
    val takenAt: Instant?,
    val isFavorite: Boolean,
) : MediaItem

fun DVideo.toModel(): Video {
    return Video(ID(id), title, path, durationMs = durationMs, size = size, bucketId = ID(bucketId), createdAt = createdAt, updatedAt = updatedAt, takenAt = takenAt, isFavorite = isFavorite)
}
