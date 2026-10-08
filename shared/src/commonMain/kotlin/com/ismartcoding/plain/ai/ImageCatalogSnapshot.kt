package com.ismartcoding.plain.ai

import kotlinx.serialization.Serializable

@Serializable
internal data class ImageCatalogSnapshot(val revision: String, val total: Int)
