package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLInput
import kotlinx.serialization.Serializable

@GraphQLInput
@Serializable
data class ImageEditorProjectInput(
    var stateB64: String,
    var thumbnail: String?,
    var canvasWidth: Int,
    var canvasHeight: Int,
    var layerCount: Int,
)
