package com.ismartcoding.plain.data

import kotlinx.serialization.Serializable

@Serializable
data class FilePathData(
    val rootPath: String,
    val fullPath: String,
    val selectedPath: String
)
