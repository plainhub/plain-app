package com.ismartcoding.plain.web.models

import com.ismartcoding.plain.data.DVideo

data class Video(
    var id: ID,
    var title: String,
    var path: String,
    val duration: Long,
    val size: Long,
    val bucketId: String,
)

fun DVideo.toModel(): Video {
    return Video(ID(id), title, path, duration, size, bucketId)
}
