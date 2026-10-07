package com.ismartcoding.plain.features.system

import kotlinx.serialization.Serializable

@Serializable
internal data class MergeStatusFacts(
    val status: String,
    val value: String?,
    val mergedSize: Long?,
    val error: String?,
)
