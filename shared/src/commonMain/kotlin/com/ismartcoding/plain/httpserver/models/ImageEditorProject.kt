package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.db.DImageEditorProject
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLField
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLType
import kotlin.time.Instant

@GraphQLType
data class ImageEditorProject(
    val id: ID,
    @GraphQLField(description = "Full editor canvas state (layer stack), base64-encoded.")
    val stateB64: String,
    @GraphQLField(description = "Preview image as a base64 data URL; null until the editor first saves a thumbnail.")
    val thumbnail: String?,
    val canvasWidth: Int,
    val canvasHeight: Int,
    val layerCount: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@GraphQLType
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
