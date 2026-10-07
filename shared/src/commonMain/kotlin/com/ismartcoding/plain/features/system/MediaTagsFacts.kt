package com.ismartcoding.plain.features.system

import kotlinx.serialization.Serializable

@Serializable
internal data class MediaTagsFacts(
    val key: String,
    val tags: List<TagFacts>,
)
