package com.ismartcoding.plain.ai

import kotlinx.serialization.Serializable

@Serializable
internal data class ImageCatalogPage(val revision: String, val items: List<ImageCatalogItem>, val nextCursor: String, val done: Boolean)
