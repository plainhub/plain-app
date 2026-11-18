package com.ismartcoding.plain.models

import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class ImageEditorProjectSummary(
    val id: String,
    val thumbnail: String?,
    val canvasWidth: Int,
    val canvasHeight: Int,
    val updatedAt: Instant,
)
