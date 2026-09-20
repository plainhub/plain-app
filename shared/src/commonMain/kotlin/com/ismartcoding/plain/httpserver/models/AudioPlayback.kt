package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.enums.MediaPlayMode
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLType

@GraphQLType
data class AudioPlayback(
    val mode: MediaPlayMode = MediaPlayMode.REPEAT,
    /** "" when idle (SDL exposes null). */
    val currentPath: String = "",
    val isPlaying: Boolean = false,
    /** Playback head of the current track; 0 when idle. */
    val positionMs: Long = 0,
)
