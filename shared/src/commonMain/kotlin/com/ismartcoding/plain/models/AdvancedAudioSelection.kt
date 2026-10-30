package com.ismartcoding.plain.models

import kotlinx.serialization.Serializable

@Serializable
data class AdvancedAudioSelection(val track: AudioItem?, val path: String, val positionMs: Long, val revision: Long)
