package com.ismartcoding.plain.platform

import kotlinx.serialization.Serializable

@Serializable
internal data class ImageModelsFiles(val imageModel: String, val textModel: String, val tokenizer: String)
