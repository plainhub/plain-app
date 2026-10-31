package com.ismartcoding.plain.features.shares

import kotlinx.serialization.Serializable

@Serializable
data class SharedFileDto(
    val name: String = "",
    val relativePath: String = "",
    val isDir: Boolean = false,
    val size: Long = 0,
    val mimeType: String = "",
    val hasThumb: Boolean = false,
)
