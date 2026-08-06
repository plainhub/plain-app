package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.features.file.DFile
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLType
import kotlin.time.Instant

@GraphQLType
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

@GraphQLType
data class Files(val dir: String, val items: List<File>)
