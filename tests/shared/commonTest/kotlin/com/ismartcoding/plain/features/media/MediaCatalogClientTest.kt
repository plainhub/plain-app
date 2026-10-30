package com.ismartcoding.plain.features.media

import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.models.SelectedMediaPath
import com.ismartcoding.plain.platform.system.DataType
import com.ismartcoding.plain.rust.graphql.GraphqlClient
import com.ismartcoding.plain.rust.graphql.GraphqlResponse
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

class MediaCatalogClientTest {
    @Test
    fun remote_share_resolves_selected_device_paths_in_requested_order() = runTest {
        val resolved = mutableListOf<String>()
        val client = MediaCatalogApi(GraphqlClient {
            GraphqlResponse(data = JsonObject(mapOf("images" to JsonHelper.jsonEncodeToElement(listOf(
                SelectedMediaPath("second", "/remote/second.png"),
                SelectedMediaPath("first", "/remote/first.png"),
            )))))
        }) { type, id, path ->
            assertEquals(DataType.IMAGE, type)
            resolved.add("$id:$path")
            "/cache/$id.png"
        }
        assertEquals(listOf("/cache/first.png", "/cache/second.png"), client.shareUris(DataType.IMAGE, listOf("first", "second")))
        assertEquals(listOf("first:/remote/first.png", "second:/remote/second.png"), resolved)
    }
}
