package com.ismartcoding.plain.data

import com.ismartcoding.plain.enums.ScreenMirrorMode
import kotlinx.serialization.Serializable



@Serializable
data class DScreenMirrorQuality(
    val mode: ScreenMirrorMode,
    val resolution: Int,
)
