package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.db.DAppFile
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLField
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLType
import kotlin.time.Instant

@GraphQLType
data class AppFile(
    @GraphQLField(description = "Content-addressable fileId (the fid suffix, `{sha256}[.{ext}]`); same value space as ChatFiles.ids/ChatImages.ids.")
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
