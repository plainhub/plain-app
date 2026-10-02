package com.ismartcoding.plain.features

import com.ismartcoding.plain.api.*
import com.ismartcoding.plain.db.DImageEditorProject
import com.ismartcoding.plain.httpserver.models.ID
import com.ismartcoding.plain.httpserver.models.ImageEditorProjectSummary
import kotlinx.serialization.json.*

object ImageEditorProjectHelper {
    private const val SUMMARY_FIELDS = "id thumbnail canvasWidth canvasHeight layerCount updatedAt"
    private const val FIELDS = "$SUMMARY_FIELDS stateB64 createdAt"

    suspend fun listAsync(limit: Int): List<ImageEditorProjectSummary> =
        RustContentApi.query("imageEditorProjectItems(offset: 0, limit: $limit, query: \"\") { $SUMMARY_FIELDS }")
            .getValue("imageEditorProjectItems").jsonArray.map { value ->
                val row = value.jsonObject
                ImageEditorProjectSummary(ID(row.string("id")), row["thumbnail"]?.jsonPrimitive?.contentOrNull,
                    row.getValue("canvasWidth").jsonPrimitive.int, row.getValue("canvasHeight").jsonPrimitive.int,
                    row.getValue("layerCount").jsonPrimitive.int, row.instant("updatedAt"))
            }

    suspend fun getByIdAsync(id: String): DImageEditorProject? =
        RustContentApi.query("imageEditorProject(id: ${gql(id)}) { $FIELDS }")
            .getValue("imageEditorProject").takeUnless { it is JsonNull }?.project()

    suspend fun addOrUpdateAsync(id: String, updateItem: DImageEditorProject.() -> Unit): DImageEditorProject {
        val project = (if (id.isEmpty()) null else getByIdAsync(id)) ?: DImageEditorProject(id)
        project.updateItem()
        val thumbnail = project.thumbnail?.let(::gql) ?: "null"
        val input = "{ stateB64: ${gql(project.stateB64)}, thumbnail: $thumbnail, canvasWidth: ${project.canvasWidth}, canvasHeight: ${project.canvasHeight}, layerCount: ${project.layerCount} }"
        return RustContentApi.mutate("saveImageEditorProject(id: ${gql(id)}, input: $input) { $FIELDS }")
            .getValue("saveImageEditorProject").project()
    }

    suspend fun deleteAsync(id: String) {
        RustContentApi.mutate("deleteImageEditorProject(id: ${gql(id)})")
    }

    suspend fun broadcastUpdate(pid: String, updateB64: String) {
        RustContentApi.mutate("broadcastImageEditorUpdate(id: ${gql(pid)}, update: ${gql(updateB64)})")
    }

    private fun JsonElement.project(): DImageEditorProject = jsonObject.let { row ->
        DImageEditorProject(row.string("id"), row.string("stateB64"), row["thumbnail"]?.jsonPrimitive?.contentOrNull,
            row.getValue("canvasWidth").jsonPrimitive.int, row.getValue("canvasHeight").jsonPrimitive.int,
            row.getValue("layerCount").jsonPrimitive.int, row.instant("createdAt"), row.instant("updatedAt"))
    }
}
