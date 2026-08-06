package com.ismartcoding.plain.httpserver.mainschemas

import com.ismartcoding.plain.features.ImageEditorProjectHelper
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLMutation
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLQuery
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.httpserver.models.ID
import com.ismartcoding.plain.httpserver.models.ImageEditorProject
import com.ismartcoding.plain.httpserver.models.ImageEditorProjectInput
import com.ismartcoding.plain.httpserver.models.ImageEditorProjectSummary
import com.ismartcoding.plain.httpserver.models.toModel
import com.ismartcoding.plain.httpserver.models.toSummary

private const val LIST_LIMIT = 20

@GraphQLQuery
suspend fun imageEditorProjects(): List<ImageEditorProjectSummary> {
    return ImageEditorProjectHelper.listAsync(LIST_LIMIT).map { it.toSummary() }
}

@GraphQLQuery
suspend fun imageEditorProject(id: ID): ImageEditorProject? {
    return ImageEditorProjectHelper.getByIdAsync(id.value)?.toModel()
}

@GraphQLMutation
suspend fun saveImageEditorProject(id: ID, input: ImageEditorProjectInput): ImageEditorProject? {
    return ImageEditorProjectHelper.addOrUpdateAsync(id.value) {
        stateB64 = input.stateB64
        thumbnail = input.thumbnail
        canvasWidth = input.canvasWidth
        canvasHeight = input.canvasHeight
        layerCount = input.layerCount
    }?.toModel()
}

@GraphQLMutation
suspend fun deleteImageEditorProject(id: ID): Boolean {
    ImageEditorProjectHelper.deleteAsync(id.value)
    return true
}

@GraphQLMutation
suspend fun broadcastImageEditorUpdate(pid: String, update: String): Boolean {
    ImageEditorProjectHelper.broadcastUpdate(pid, update)
    return true
}

fun SchemaBuilder.addImageEditorProjectSchema() {
}
