package com.ismartcoding.plain.models

import com.ismartcoding.plain.features.audio.MediaPlayMode
import com.ismartcoding.plain.preferences.PrefKeys
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull
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

internal fun audioPlayMode(prefs: Map<String, JsonElement>): MediaPlayMode =
    prefs[PrefKeys.AUDIO_PLAY_MODE]?.jsonPrimitive?.contentOrNull?.let(MediaPlayMode::valueOf)
        ?: MediaPlayMode.REPEAT
