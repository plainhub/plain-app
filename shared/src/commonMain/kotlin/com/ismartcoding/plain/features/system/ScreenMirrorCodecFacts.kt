package com.ismartcoding.plain.features.system

import kotlinx.serialization.Serializable

@Serializable
internal data class ScreenMirrorCodecFacts(
    val annexB: String,
    val keyFrame: String?,
)
