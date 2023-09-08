package com.ismartcoding.plain.web.models

import com.ismartcoding.lib.extensions.getFilenameFromPath
import com.ismartcoding.plain.features.file.DFile
import com.ismartcoding.plain.features.image.DImage
import com.ismartcoding.plain.helpers.FileHelper
import kotlinx.datetime.Instant

data class Image(
    var id: ID,
    var title: String,
    var path: String,
    val size: Long,
    val bucketId: String
)

fun DImage.toModel(): Image {
    return Image(ID(id), title, path, size, bucketId)
}
