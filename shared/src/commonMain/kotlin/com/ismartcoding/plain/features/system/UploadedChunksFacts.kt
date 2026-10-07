package com.ismartcoding.plain.features.system

import kotlinx.serialization.Serializable

@Serializable
internal data class UploadedChunksFacts(
    val chunks: List<String>,
)
