package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.data.DVideo
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLType
import kotlin.time.Instant

@GraphQLType
data class Video(
    override var id: ID,
    override var title: String,
    override var path: String,
    val duration: Long,
    override val size: Long,
    override val bucketId: String,
    override val createdAt: Instant,
    override val updatedAt: Instant,
    val takenAt: Instant?,
    val isFavorite: Boolean,
) : MediaItem

fun DVideo.toModel(): Video {
    return Video(ID(id), title, path, duration, size, bucketId, createdAt, updatedAt, takenAt, isFavorite)
}
