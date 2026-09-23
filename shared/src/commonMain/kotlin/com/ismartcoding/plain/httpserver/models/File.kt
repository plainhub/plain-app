package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.features.file.DFile
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLField
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLType
import kotlin.time.Instant

@GraphQLType
data class File(
    var name: String,
    val path: String,
    @GraphQLField(description = "Creation time from stat; null when the platform cannot report it (some filesystems/IO errors).")
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

@GraphQLType
data class Files(val dir: String, val items: List<File>)
