package com.ismartcoding.plain.data

import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLInput
import kotlinx.serialization.Serializable

@GraphQLInput
@Serializable
data class TouchPointInput(
    val x: Float,
    val y: Float,
    val t: Int,
)
