package com.ismartcoding.plain.features.system

import kotlinx.serialization.Serializable

@Serializable
internal data class AudioInfoFacts(
    val durationMs: Long,
    val location: LocationFacts?,
)
