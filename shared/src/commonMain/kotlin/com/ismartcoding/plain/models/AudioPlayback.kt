package com.ismartcoding.plain.models

import kotlinx.serialization.Serializable

@Serializable
data class AudioPlayback(
    var mediaId: String?,
    var path: String?,
)
