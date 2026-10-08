package com.ismartcoding.plain.features.dlna

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
enum class DlnaPlaybackState {
    @SerialName("NoMediaPresent") NO_MEDIA_PRESENT,
    @SerialName("Stopped") STOPPED,
    @SerialName("Playing") PLAYING,
    @SerialName("PausedPlayback") PAUSED,
    @SerialName("Transitioning") TRANSITIONING,
}
