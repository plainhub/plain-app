package com.ismartcoding.plain.web.schemas

import com.ismartcoding.plain.features.ImageEditorProjectHelper
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.web.models.ID
import com.ismartcoding.plain.web.models.ImageEditorProjectInput
import com.ismartcoding.plain.web.models.toModel
import com.ismartcoding.plain.web.models.toSummary

private const val LIST_LIMIT = 20

fun SchemaBuilder.addImageEditorProjectSchema() {
    query("imageEditorProjects") {
        resolver { ->
            ImageEditorProjectHelper.listAsync(LIST_LIMIT).map { it.toSummary() }
        }
    }
    query("imageEditorProject") {
        resolver("id") { id: ID ->
            ImageEditorProjectHelper.getByIdAsync(id.value)?.toModel()
        }
    }
    mutation("saveImageEditorProject") {
        resolver("id", "input") { id: ID, input: ImageEditorProjectInput ->
            ImageEditorProjectHelper.addOrUpdateAsync(id.value) {
                stateB64 = input.stateB64
                thumbnail = input.thumbnail
                canvasWidth = input.canvasWidth
                canvasHeight = input.canvasHeight
                layerCount = input.layerCount
            }?.toModel()
        }
    }
    mutation("deleteImageEditorProject") {
        resolver("id") { id: ID ->
            ImageEditorProjectHelper.deleteAsync(id.value)
            true
        }
    }
    mutation("broadcastImageEditorUpdate") {
        resolver("pid", "update") { pid: String, update: String ->
            ImageEditorProjectHelper.broadcastUpdate(pid, update)
            true
        }
    }
}
