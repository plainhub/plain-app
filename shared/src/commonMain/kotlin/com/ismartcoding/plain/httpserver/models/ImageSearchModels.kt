package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.ai.ImageSearchStatusType
import kotlinx.serialization.Serializable

@Serializable
data class ImageSearchStatus(
    val status: ImageSearchStatusType,
    val downloadProgress: Int,
    val errorMessage: String,
    val modelSize: Long,
    val modelDir: String,
    val isIndexing: Boolean,
    val totalImages: Int,
    val indexedImages: Int,
)
