package com.ismartcoding.plain.tests

import kotlinx.serialization.json.JsonObject
import com.ismartcoding.plain.rust.graphql.GraphqlRequest
import com.ismartcoding.plain.rust.graphql.GraphqlApi

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismartcoding.plain.features.images.editor.ImageEditorProjectsClient
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImageEditorRustHttpTest {
    @Test
    fun canvasProjectsUseRustAndListsReturnSummaries() = runBlocking {
        val id = "image-rust-${UUID.randomUUID()}"
        try {
            val saved = ImageEditorProjectsClient.addOrUpdateAsync(id) {
                stateB64 = "AQID"
                thumbnail = "data:image/jpeg;base64,AQID"
                canvasWidth = 100
                canvasHeight = 200
                layerCount = 2
            }
            val summary = ImageEditorProjectsClient.listAsync().first { it.id.value == id }
            assertEquals(saved.thumbnail, summary.thumbnail)
            assertEquals(100, summary.canvasWidth)
            val updated = ImageEditorProjectsClient.addOrUpdateAsync(id) { stateB64 = "BAU="; layerCount = 3 }
            assertEquals(saved.createdAt, updated.createdAt)
            assertEquals(saved.thumbnail, updated.thumbnail)
            assertEquals("BAU=", checkNotNull(ImageEditorProjectsClient.getByIdAsync(id)).stateB64)
            GraphqlApi.execute(GraphqlRequest<JsonObject>("mutation(\$id: ID!, \$update: String!) { broadcastImageEditorUpdate(id: \$id, update: \$update) }", mapOf("id" to JsonPrimitive(id), "update" to JsonPrimitive("AQID"))))
            ImageEditorProjectsClient.deleteAsync(id)
            assertNull(ImageEditorProjectsClient.getByIdAsync(id))
        } finally {
            ImageEditorProjectsClient.deleteAsync(id)
        }
    }
}
