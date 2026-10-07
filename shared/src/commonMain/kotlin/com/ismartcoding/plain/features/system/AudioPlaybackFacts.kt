package com.ismartcoding.plain.features.system

import kotlinx.serialization.Serializable

@Serializable
internal data class AudioPlaybackFacts(
    val isPlaying: Boolean,
    val positionMs: Long,
)
