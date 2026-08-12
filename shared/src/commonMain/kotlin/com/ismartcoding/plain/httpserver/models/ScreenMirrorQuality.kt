package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.data.DScreenMirrorQuality
import com.ismartcoding.plain.enums.ScreenMirrorMode
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLType

@GraphQLType
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
