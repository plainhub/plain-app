package com.ismartcoding.plain.web.models

import android.util.Base64
import kotlinx.serialization.Serializable

@Serializable
data class ScreenMirrorVideoCodec(
    val annexB: String,
    val keyFrame: String? = null,
)
