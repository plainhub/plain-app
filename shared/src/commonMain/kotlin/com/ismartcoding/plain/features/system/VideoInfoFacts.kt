package com.ismartcoding.plain.features.system

import kotlinx.serialization.Serializable

@Serializable
data class VideoInfoFacts(
    val width: Int,
    val height: Int,
    val durationMs: Long,
    val rawLocation: String?,
)
