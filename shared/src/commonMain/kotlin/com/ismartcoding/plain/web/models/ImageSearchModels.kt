package com.ismartcoding.plain.web.models

import kotlinx.serialization.Serializable

@Serializable
data class ImageSearchStatus(
    val status: String,
    val downloadProgress: Int,
    val errorMessage: String,
    val modelSize: Long,
    val modelDir: String,
    val isIndexing: Boolean,
    val totalImages: Int,
    val indexedImages: Int,
)
