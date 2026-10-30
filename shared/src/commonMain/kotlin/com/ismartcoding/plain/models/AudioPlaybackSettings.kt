package com.ismartcoding.plain.models

import com.ismartcoding.plain.preferences.PrefKeys
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonPrimitive

@Serializable
internal data class AudioPlaybackSettings(
    val audioPlayback: AudioPlayback,
    val prefs: Map<String, JsonElement>,
) {
    val speed: Float get() = audioPlaybackSpeed(prefs)
}

internal fun audioPlaybackSpeed(prefs: Map<String, JsonElement>): Float =
    prefs[PrefKeys.AUDIO_PLAYBACK_SPEED]?.jsonPrimitive?.float ?: 1f
