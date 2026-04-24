package com.ismartcoding.plain.web.models

import com.ismartcoding.plain.docs.DDoc
import kotlin.time.Instant

data class Doc(
    val id: ID,
    val title: String,
    val path: String,
    val extension: String,
    val size: Long,
    val bucketId: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class DocExtGroup(
    val ext: String,
    val count: Int,
)

fun DDoc.toDocModel(): Doc {
    return Doc(ID(id), title, path, extension, size, bucketId, createdAt, updatedAt)
}
