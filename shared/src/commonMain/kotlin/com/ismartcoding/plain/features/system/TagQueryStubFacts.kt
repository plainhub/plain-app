package com.ismartcoding.plain.features.system

import kotlinx.serialization.Serializable

@Serializable
internal data class TagQueryStubFacts(
    val key: String,
    val title: String,
    val size: Long,
)
