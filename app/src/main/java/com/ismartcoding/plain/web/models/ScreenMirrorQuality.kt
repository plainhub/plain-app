package com.ismartcoding.plain.web.models

import com.ismartcoding.plain.data.DScreenMirrorQuality

data class ScreenMirrorQuality(
    val quality: Int,
    val resolution: Int
)

fun DScreenMirrorQuality.toModel(): ScreenMirrorQuality {
    return ScreenMirrorQuality(
        quality, resolution
    )
}
