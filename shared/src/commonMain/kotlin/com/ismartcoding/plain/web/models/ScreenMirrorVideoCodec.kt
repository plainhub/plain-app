package com.ismartcoding.plain.web.models

import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLType
import kotlinx.serialization.Serializable

@GraphQLType
@Serializable
data class ScreenMirrorVideoCodec(
    val annexB: String,
    val keyFrame: String? = null,
)
