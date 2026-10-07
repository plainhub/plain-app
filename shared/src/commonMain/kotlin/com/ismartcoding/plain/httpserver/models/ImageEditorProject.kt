package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.db.DImageEditorProject
import kotlin.time.Instant

data class ImageEditorProjectSummary(
    val id: ID,
    val thumbnail: String?,
    val canvasWidth: Int,
    val canvasHeight: Int,
    val layerCount: Int,
    val updatedAt: Instant,
)

fun DImageEditorProject.toSummary(): ImageEditorProjectSummary {
    return ImageEditorProjectSummary(
        ID(id),
        thumbnail,
        canvasWidth,
        canvasHeight,
        layerCount,
        updatedAt,
    )
}
