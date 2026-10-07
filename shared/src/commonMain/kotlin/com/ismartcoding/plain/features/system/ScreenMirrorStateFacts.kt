package com.ismartcoding.plain.features.system

import kotlinx.serialization.Serializable

@Serializable
internal data class ScreenMirrorStateFacts(
    val running: Boolean,
    val controlEnabled: Boolean,
    val codec: ScreenMirrorCodecFacts?,
)
