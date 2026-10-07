package com.ismartcoding.plain.features.system

import kotlinx.serialization.Serializable

@Serializable
internal data class FileMetadataFacts(
    val size: Long,
    val mimeType: String,
)
