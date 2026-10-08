package com.ismartcoding.plain.ai

import kotlinx.serialization.Serializable

@Serializable
internal data class ImageEmbeddedItem(val id: String, val path: String, val embeddingBase64: String)
