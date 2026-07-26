package com.ismartcoding.plain.ui.components.mediaviewer

data class PlaybackSpeed(val speed: Float, val label: String)

val PLAYBACK_SPEEDS = listOf(
    PlaybackSpeed(0.25f, "0.25x"),
    PlaybackSpeed(0.5f, "0.5x"),
    PlaybackSpeed(1f, "1x"),
    PlaybackSpeed(2f, "2x"),
    PlaybackSpeed(3f, "3x"),
)
