package com.ismartcoding.plain.ai

import kotlinx.serialization.Serializable

@Serializable
internal data class ImageEmbeddedBatch(val items: List<ImageEmbeddedItem>, val skippedIds: List<String>)
