package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLField
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLType
import kotlinx.serialization.Serializable

@GraphQLType
@Serializable
data class ScreenMirrorVideoCodec(
    @GraphQLField(description = "H.264 Annex-B codec configuration (SPS/PPS), base64-encoded.")
    val annexB: String,
    @GraphQLField(description = "Latest H.264 keyframe, base64-encoded; null until the pipeline emits one.")
    val keyFrame: String? = null,
)
