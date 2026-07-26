package com.ismartcoding.plain.web.models

import kotlinx.serialization.Serializable

@Serializable
data class ImageEditorProjectInput(
    var stateB64: String,
    var thumbnail: String?,
    var canvasWidth: Int,
    var canvasHeight: Int,
    var layerCount: Int,
)
