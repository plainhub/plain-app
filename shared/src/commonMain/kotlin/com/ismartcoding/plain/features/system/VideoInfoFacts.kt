package com.ismartcoding.plain.features.system

import kotlinx.serialization.Serializable

@Serializable
internal data class VideoInfoFacts(
    val width: Int,
    val height: Int,
    val durationMs: Long,
    val location: LocationFacts?,
)
