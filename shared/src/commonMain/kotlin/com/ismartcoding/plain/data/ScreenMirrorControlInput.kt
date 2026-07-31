package com.ismartcoding.plain.data

import com.ismartcoding.plain.enums.ScreenMirrorControlAction
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLInput
import kotlinx.serialization.Serializable

/**
 * Input data from the web client for screen mirror remote control.
 * Coordinates (x, y, endX, endY) are normalized to [0, 1].
 */
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
)
