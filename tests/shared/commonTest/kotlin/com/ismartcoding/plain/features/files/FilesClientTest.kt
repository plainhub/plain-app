package com.ismartcoding.plain.features.files

import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.models.File
import com.ismartcoding.plain.rust.graphql.GraphqlClient
import com.ismartcoding.plain.rust.graphql.GraphqlResponse
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class FilesClientTest {
    @Test
    fun sends_the_decoded_file_uri_path_to_the_write_mutation() = runTest {
        val expected = "/tmp/示例 +%#.txt"
        val file = File(
            name = "示例 +%#.txt", path = expected, createdAt = null,
            updatedAt = Instant.fromEpochMilliseconds(0), size = 4, isDir = false, childCount = 0,
        )
        val api = GraphqlClient { operation ->
            assertEquals(JsonPrimitive(expected), operation.variables["path"])
            GraphqlResponse(data = JsonHelper.jsonEncodeToElement(mapOf("writeTextFile" to file)).jsonObject)
        }
        val client = FilesApi(api, upload = { _, _ -> error("Unexpected upload") })

        assertEquals(expected, client.writeText("file:///tmp/%E7%A4%BA%E4%BE%8B%20+%25%23.txt", "test", false).path)
    }
}
