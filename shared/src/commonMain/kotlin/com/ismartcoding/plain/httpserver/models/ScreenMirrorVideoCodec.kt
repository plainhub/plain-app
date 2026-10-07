package com.ismartcoding.plain.httpserver.models

import kotlinx.serialization.Serializable

@Serializable
data class ScreenMirrorVideoCodec(
    val annexB: String,
    val keyFrame: String? = null,
)
