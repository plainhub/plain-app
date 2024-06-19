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

fun java.io.File.toModel(): File {
    return File(
        name = name,
        path = absolutePath,
        permission = "rw",
        createdAt = null,
        updatedAt = lastModified().let { Instant.fromEpochMilliseconds(it) },
        size = length(),
        isDir = isDirectory,
        children = if (isDirectory) listFiles()?.size ?: 0 else 0,
        mediaId = ""
    )
}

data class Files(val dir: String, val items: List<File>)
