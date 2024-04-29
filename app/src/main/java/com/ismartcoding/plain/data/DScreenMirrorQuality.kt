package com.ismartcoding.plain.data

import kotlinx.serialization.Serializable

@Serializable
data class DScreenMirrorQuality(
    val quality: Int = 50,
    val resolution: Int = 720 // 480p, 720p，1080p，4k
)