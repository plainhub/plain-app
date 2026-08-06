package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.db.DAppFile
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLType
import kotlin.time.Instant

@GraphQLType
data class AppFile(
    val id: ID,
    val size: Long,
    val mimeType: String,
    val realPath: String,
    val fileName: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

fun DAppFile.toModel(fileName: String): AppFile {
    return AppFile(ID(id), size, mimeType, realPath, fileName, createdAt, updatedAt)
}
