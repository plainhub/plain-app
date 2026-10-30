package com.ismartcoding.plain.models

import kotlinx.serialization.Serializable

@Serializable
data class MergeResult(
    val status: MergeTaskStatus,
    val error: String? = null,
    val value: String? = null,
)
