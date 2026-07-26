package com.ismartcoding.plain.web.models

import com.ismartcoding.plain.db.DImageEditorProject
import kotlin.time.Instant

data class ImageEditorProject(
    val id: ID,
    val stateB64: String,
    val thumbnail: String?,
    val canvasWidth: Int,
    val canvasHeight: Int,
    val layerCount: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class ImageEditorProjectSummary(
    val id: ID,
    val thumbnail: String?,
    val canvasWidth: Int,
    val canvasHeight: Int,
    val layerCount: Int,
    val updatedAt: Instant,
)

fun DImageEditorProject.toModel(): ImageEditorProject {
    return ImageEditorProject(
        ID(id),
        stateB64,
        thumbnail,
        canvasWidth,
        canvasHeight,
        layerCount,
        createdAt,
        updatedAt,
    )
}

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
