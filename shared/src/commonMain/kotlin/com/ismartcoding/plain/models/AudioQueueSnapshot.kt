package com.ismartcoding.plain.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
internal data class AudioQueueSnapshot(
    val audioPlayback: AudioPlayback,
    val prefs: Map<String, JsonElement>,
    val count: Int,
    val items: List<AudioItem>,
    val queuedIds: List<QueuedMediaId>,
)
