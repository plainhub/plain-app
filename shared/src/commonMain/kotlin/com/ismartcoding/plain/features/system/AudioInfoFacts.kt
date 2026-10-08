package com.ismartcoding.plain.features.system

import kotlinx.serialization.Serializable

@Serializable
data class AudioInfoFacts(
    val durationMs: Long,
    val rawLocation: String?,
)
