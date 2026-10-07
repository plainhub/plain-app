package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.features.file.DFile
import kotlin.time.Instant

data class File(
    var name: String,
    val path: String,
    val createdAt: Instant?,
    val updatedAt: Instant,
    val size: Long,
    val isDir: Boolean,
    val childCount: Int,
    val mediaId: String
)

fun DFile.toModel(): File {
    return File(name, path, createdAt, updatedAt, size, isDir, childCount, mediaId)
}

data class Files(val dir: String, val items: List<File>)
