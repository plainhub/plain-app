package com.ismartcoding.plain.web.models

import com.ismartcoding.lib.extensions.getFilenameFromPath
import com.ismartcoding.plain.features.file.DFile
import com.ismartcoding.plain.features.image.DImage
import com.ismartcoding.plain.features.video.DVideo
import com.ismartcoding.plain.helpers.FileHelper
import kotlinx.datetime.Instant

data class Video(
    var id: ID,
    var title: String,
    var path: String,
    val duration: Long,
    val size: Long,
    val bucketId: String
)

fun DVideo.toModel(): Video {
    return Video(ID(id), title, path, duration, size, bucketId)
}
