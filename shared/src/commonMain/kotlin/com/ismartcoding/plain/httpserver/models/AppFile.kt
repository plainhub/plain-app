package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.db.DAppFile
import kotlin.time.Instant

data class AppFile(
    val id: String,
    val size: Long,
    val mimeType: String,
    val realPath: String,
    val fileName: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

fun DAppFile.toModel(fileName: String): AppFile {
    return AppFile(id, size, mimeType, realPath, fileName, createdAt, updatedAt)
}
