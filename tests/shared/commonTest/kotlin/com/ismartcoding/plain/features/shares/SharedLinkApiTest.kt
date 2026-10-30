package com.ismartcoding.plain.features.shares

import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.rust.graphql.GraphqlClient
import com.ismartcoding.plain.rust.graphql.GraphqlOperation
import com.ismartcoding.plain.rust.graphql.GraphqlResponse
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SharedLinkApiTest {
    @Test
    fun preview_and_thumbnail_paths_remain_owned_by_the_supplied_device() = runTest {
        val operations = mutableListOf<GraphqlOperation>()
        val client = SharedLinkApi(GraphqlClient {
            operations.add(it)
            GraphqlResponse(data = JsonHelper.jsonDecode("""{"cacheSharedFile":"/target-cache/photo.jpg"}"""))
        })
        val link = SharedLink("device.test", 8443, "share", "token")
        val entry = SharedFileDto("photo.jpg", "/photos/photo.jpg", mimeType = "image/jpeg")
        for (thumbnail in listOf(false, true)) {
            assertEquals("/target-cache/photo.jpg", client.cacheFile(link, "url-token", entry, thumbnail))
            val operation = operations.last()
            assertEquals(JsonHelper.jsonEncodeToElement(link), operation.variables["link"])
            assertEquals(JsonHelper.jsonEncodeToElement(entry), operation.variables["entry"])
            assertEquals(JsonPrimitive("url-token"), operation.variables["urlToken"])
            assertEquals(JsonPrimitive(thumbnail), operation.variables["thumbnail"])
        }
        assertEquals(2, operations.size)
    }

    @Test
    fun target_cache_failure_is_returned_without_a_native_fallback() = runTest {
        var calls = 0
        val client = SharedLinkApi(GraphqlClient {
            calls++
            error("target unavailable")
        })
        assertEquals("target unavailable", assertFailsWith<IllegalStateException> {
            client.cacheFile(SharedLink("device.test", 8443, "share", "token"), "url-token", SharedFileDto("file.txt"))
        }.message)
        assertEquals(1, calls)
    }
}
