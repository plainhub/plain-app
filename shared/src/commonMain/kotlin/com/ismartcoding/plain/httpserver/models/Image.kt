package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.data.DImage
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLType
import kotlin.time.Instant

@GraphQLType
data class Image(
    var id: ID,
    var title: String,
    var path: String,
    val size: Long,
    val bucketId: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val takenAt: Instant?,
    val isFavorite: Boolean,
)

fun DImage.toModel(): Image {
    return Image(ID(id), title, path, size, bucketId, createdAt, updatedAt, takenAt, isFavorite)
}
