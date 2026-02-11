package com.ismartcoding.plain.web.models

import com.ismartcoding.plain.data.DScreenMirrorQuality

data class ScreenMirrorQuality(
    val mode: String,
    val resolution: Int,
)

fun DScreenMirrorQuality.toModel(): ScreenMirrorQuality {
    return ScreenMirrorQuality(
        mode = mode.name,
        resolution = resolution,
    )
}
