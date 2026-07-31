package com.ismartcoding.plain.web.models

import com.ismartcoding.plain.data.DVideo
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLType
import kotlin.time.Instant

@GraphQLType
data class Video(
    var id: ID,
    var title: String,
    var path: String,
    val duration: Long,
    val size: Long,
    val bucketId: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val takenAt: Instant?,
    val isFavorite: Boolean,
)

fun DVideo.toModel(): Video {
    return Video(ID(id), title, path, duration, size, bucketId, createdAt, updatedAt, takenAt, isFavorite)
}
