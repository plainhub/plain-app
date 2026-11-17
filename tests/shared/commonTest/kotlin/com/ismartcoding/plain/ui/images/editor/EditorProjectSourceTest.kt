package com.ismartcoding.plain.ui.images.editor

import com.ismartcoding.plain.features.images.editor.ImageEditorProjectsClient
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.models.ImageEditorProject
import com.ismartcoding.plain.rust.graphql.GraphqlClient
import com.ismartcoding.plain.rust.graphql.GraphqlOperation
import com.ismartcoding.plain.rust.graphql.GraphqlResponse
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import kotlin.test.*

class EditorProjectSourceTest {
    @Test
    fun everyProjectOperationUsesTheSelectedSourceAndPreservesItsThumbnail() = runTest {
        val row = ImageEditorProject("019f4b00-0000-7000-8000-000000000001", stateB64 = "AA==", thumbnail = "owned-thumbnail", canvasWidth = 640, canvasHeight = 480, layerCount = 9)
        val calls = mutableListOf<GraphqlOperation>()
        val api = GraphqlClient("owned-fixture") { request ->
            calls.add(request)
            val data = when {
                request.query.contains("saveImageEditorProject(") -> {
                    val input = request.variables.getValue("input").jsonObject
                    assertEquals("owned-thumbnail", input.getValue("thumbnail").jsonPrimitive.content)
                    JsonObject(mapOf("saveImageEditorProject" to JsonHelper.jsonEncodeToElement(row.copy(stateB64 = input.getValue("stateB64").jsonPrimitive.content))))
                }
                request.query.contains("deleteImageEditorProject(") -> JsonObject(mapOf("deleteImageEditorProject" to JsonPrimitive(true)))
                request.query.contains("imageEditorProjects ") -> JsonObject(mapOf("imageEditorProjects" to JsonArray(listOf(JsonHelper.jsonEncodeToElement(row)))))
                else -> JsonObject(mapOf("imageEditorProject" to JsonHelper.jsonEncodeToElement(row)))
            }
            GraphqlResponse(data = data)
        }
        val projects = ImageEditorProjectsClient(api)
        assertEquals(row.id, projects.listAsync().single().id.value)
        assertEquals(row, projects.getByIdAsync(row.id))
        assertEquals("AQ==", projects.addOrUpdateAsync(row.id) { stateB64 = "AQ==" }.stateB64)
        projects.deleteAsync(row.id)
        assertEquals(5, calls.size)
        assertEquals(1, calls.count { it.query.contains("saveImageEditorProject(") })
        assertEquals(1, calls.count { it.query.contains("deleteImageEditorProject(") })
    }
}
