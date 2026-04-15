package com.ismartcoding.plain.web.models

import com.ismartcoding.plain.features.file.DFile
import kotlin.time.Instant

data class Doc(
    val id: ID,
    val name: String,
    val path: String,
    val extension: String,
    val size: Long,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class DocExtGroup(
    val ext: String,
    val count: Int,
)

fun DFile.toDocModel(): Doc {
    val ext = name.substringAfterLast('.', "").lowercase()
    return Doc(ID(mediaId), name, path, ext, size, createdAt ?: updatedAt, updatedAt)
}
