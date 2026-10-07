package com.ismartcoding.plain.features.system

import kotlinx.serialization.Serializable

@Serializable
internal data class FileFacts(
    val name: String,
    val path: String,
    val mediaId: String,
    val createdAt: Long?,
    val updatedAt: Long,
    val size: Long,
    val isDir: Boolean,
    val childCount: Int,
)
