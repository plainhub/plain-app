package com.ismartcoding.plain.features.system

import kotlinx.serialization.Serializable

@Serializable
internal data class MediaBucketItemFacts(
    val id: String,
    val name: String,
    val size: Long,
    val path: String,
    val sortName: String,
)
