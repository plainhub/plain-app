package com.ismartcoding.plain.ai

import kotlinx.serialization.Serializable

@Serializable
internal data class ImageModelsSnapshot(
    val version: Long = 0,
    val status: ImageSearchStatusType = ImageSearchStatusType.UNAVAILABLE,
    val downloadProgress: Int = 0, val errorMessage: String = "", val modelSize: Long = 0, val modelDir: String = "",
    val isIndexing: Boolean = false, val totalImages: Int = 0, val indexedImages: Int = 0,
)
