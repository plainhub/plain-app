package com.ismartcoding.plain.web.models

import com.ismartcoding.plain.features.file.DFile
import kotlinx.datetime.Instant

data class File(
    var name: String,
    val path: String,
    val permission: String,
    val updatedAt: Instant,
    val size: Long,
    val isDir: Boolean,
)

fun DFile.toModel(): File {
    return File(name, path, permission, updatedAt, size, isDir)
}

data class Files(val dir: String, val items: List<File>)
