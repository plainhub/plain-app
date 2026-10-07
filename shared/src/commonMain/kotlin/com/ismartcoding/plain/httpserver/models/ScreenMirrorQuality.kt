package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.data.DScreenMirrorQuality
import com.ismartcoding.plain.enums.ScreenMirrorMode

data class ScreenMirrorQuality(
    val mode: ScreenMirrorMode,
    val resolution: Int,
)

fun DScreenMirrorQuality.toModel(): ScreenMirrorQuality {
    return ScreenMirrorQuality(
        mode = mode,
        resolution = resolution,
    )
}
