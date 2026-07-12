package com.ismartcoding.plain.web.models

import kotlinx.serialization.Serializable

@Serializable
data class ScreenMirrorVideoCodec(
    val annexB: String,
    val keyFrame: String? = null,
)
