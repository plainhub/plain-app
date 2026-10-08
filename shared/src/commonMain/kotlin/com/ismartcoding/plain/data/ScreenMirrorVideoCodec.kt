package com.ismartcoding.plain.data

import kotlinx.serialization.Serializable

@Serializable
data class ScreenMirrorVideoCodec(
    val annexB: String,
    val keyFrame: String? = null,
)
