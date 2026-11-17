package com.ismartcoding.plain.features.dlna

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
enum class DlnaPlaybackState {
    @SerialName("NO_MEDIA_PRESENT") NO_MEDIA_PRESENT,
    @SerialName("STOPPED") STOPPED,
    @SerialName("PLAYING") PLAYING,
    @SerialName("PAUSED_PLAYBACK") PAUSED,
    @SerialName("TRANSITIONING") TRANSITIONING,
}
