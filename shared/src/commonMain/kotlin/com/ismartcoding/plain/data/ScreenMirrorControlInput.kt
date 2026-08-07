package com.ismartcoding.plain.data

import com.ismartcoding.plain.enums.ScreenMirrorControlAction
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLInput
import kotlinx.serialization.Serializable

@GraphQLInput
@Serializable
data class ScreenMirrorControlInput(
    val action: ScreenMirrorControlAction,
    val x: Float? = null,
    val y: Float? = null,
    val endX: Float? = null,
    val endY: Float? = null,
    val duration: Long? = null,
    val deltaX: Float? = null,
    val deltaY: Float? = null,
    val key: String? = null,
    val pathPoints: List<TouchPointInput>? = null,
    val pointerId: Int? = null,
    val pressure: Float? = null,
)
