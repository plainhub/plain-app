package com.ismartcoding.plain.web.models

import com.ismartcoding.plain.features.file.DFile
import kotlinx.datetime.Instant

data class File(
    var name: String,
    val path: String,
    val permission: String,
    val createdAt: Instant?,
    val updatedAt: Instant,
    val size: Long,
    val isDir: Boolean,
    val children: Int,
    val mediaId: String
)

fun DFile.toModel(): File {
    return File(name, path, permission, createdAt, updatedAt, size, isDir, children, mediaId)
}

data class Files(val dir: String, val items: List<File>)
