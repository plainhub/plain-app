package com.ismartcoding.plain.data

import kotlinx.serialization.Serializable

@Serializable
data class DFavoriteFolder(
    val rootPath: String,
    val fullPath: String,
    val alias: String? = null,
)