package com.ismartcoding.plain.features.system

import kotlinx.serialization.Serializable

@Serializable
internal data class MountFacts(
    val id: String,
    val name: String,
    val path: String,
    val mountPoint: String,
    val fsType: String,
    val totalBytes: Long,
    val usedBytes: Long,
    val freeBytes: Long,
    val remote: Boolean,
    val alias: String,
    val driveType: String,
    val diskId: String,
)
