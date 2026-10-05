package com.ismartcoding.plain.data

import kotlinx.serialization.Serializable

@Serializable
data class DMediaBucketItemFact(
    val id: String,
    val name: String,
    val size: Long,
    val path: String,
    val sortName: String,
)
