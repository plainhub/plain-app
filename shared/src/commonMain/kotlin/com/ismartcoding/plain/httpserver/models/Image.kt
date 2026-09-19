package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.data.DImage
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLType
import kotlin.time.Instant

@GraphQLType
data class Image(
    override var id: ID,
    override var title: String,
    override var path: String,
    override val size: Long,
    override val bucketId: String,
    override val createdAt: Instant,
    override val updatedAt: Instant,
    val takenAt: Instant?,
    val isFavorite: Boolean,
) : MediaItem

fun DImage.toModel(): Image {
    return Image(ID(id), title, path, size, bucketId, createdAt, updatedAt, takenAt, isFavorite)
}
