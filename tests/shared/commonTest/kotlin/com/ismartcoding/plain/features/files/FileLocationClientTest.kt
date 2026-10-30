package com.ismartcoding.plain.features.files

import com.ismartcoding.plain.rust.graphql.GraphqlClient
import com.ismartcoding.plain.rust.graphql.GraphqlResponse
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FileLocationClientTest {
    private fun client(): FileLocationApi {
        val values = mutableMapOf<String, JsonElement>()
        return FileLocationApi(GraphqlClient { operation ->
            val key = operation.variables.getValue("key").jsonPrimitive.content
            assertTrue(key in setOf("last_file_path", "recent_save_dirs"))
            val result = if ("setSavedValue" in operation.query) {
                values[key] = operation.variables.getValue("value")
                "setSavedValue" to JsonPrimitive(true)
            } else "savedValue" to (values[key] ?: JsonNull)
            GraphqlResponse(data = JsonObject(mapOf(result)))
        })
    }

    @Test
    fun device_locations_use_supported_keys_and_keep_independent_state() = runTest {
        val local = client()
        val remote = client()
        val localPath = FilePathData("/local", "/local", "/local/docs")
        val remotePath = FilePathData("/remote", "/remote", "/remote/docs")
        assertEquals(FilePathData("", "", ""), remote.lastPath())
        local.saveLastPath(localPath)
        remote.saveLastPath(remotePath)
        assertEquals(localPath, local.lastPath())
        assertEquals(remotePath, remote.lastPath())
        local.recordSaveDir("/local/docs")
        remote.recordSaveDir("/remote/first")
        remote.recordSaveDir("/remote/second")
        remote.recordSaveDir("/remote/first")
        assertEquals(listOf("/local/docs"), local.recentSaveDirs())
        assertEquals(listOf("/remote/first", "/remote/second"), remote.recentSaveDirs())
    }
}
