package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.data.DDoc
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLType
import kotlin.time.Instant

@GraphQLType
data class Doc(
    override val id: ID,
    override val title: String,
    override val path: String,
    val extension: String,
    override val size: Long,
    override val bucketId: String,
    override val createdAt: Instant,
    override val updatedAt: Instant,
) : MediaItem

@GraphQLType
data class DocExtGroup(
    val ext: String,
    val count: Int,
)

fun DDoc.toDocModel(): Doc {
    return Doc(ID(id), title, path, extension, size, bucketId, createdAt, updatedAt)
}
