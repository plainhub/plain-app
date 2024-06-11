package com.ismartcoding.plain.web.models

import com.ismartcoding.plain.data.DVideo
import kotlinx.datetime.Instant

data class Video(
    var id: ID,
    var title: String,
    var path: String,
    val duration: Long,
    val size: Long,
    val bucketId: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

fun DVideo.toModel(): Video {
    return Video(ID(id), title, path, duration, size, bucketId, createdAt, updatedAt)
}
