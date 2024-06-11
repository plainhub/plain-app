package com.ismartcoding.plain.web.models

import com.ismartcoding.plain.data.DImage
import kotlinx.datetime.Instant

data class Image(
    var id: ID,
    var title: String,
    var path: String,
    val size: Long,
    val bucketId: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

fun DImage.toModel(): Image {
    return Image(ID(id), title, path, size, bucketId, createdAt, updatedAt)
}
