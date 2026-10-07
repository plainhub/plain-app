package com.ismartcoding.plain.features.system

import kotlinx.serialization.Serializable

@Serializable
internal data class PlaylistTrackFacts(
    val title: String,
    val artist: String,
    val path: String,
    val durationMs: Long,
)
