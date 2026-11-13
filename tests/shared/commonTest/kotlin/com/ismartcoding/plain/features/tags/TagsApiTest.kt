package com.ismartcoding.plain.features.tags

import com.ismartcoding.plain.rust.graphql.graphqlClient
import kotlinx.serialization.json.*
import com.ismartcoding.plain.rust.graphql.GraphqlOperation
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.platform.system.DataType
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TagsApiTest {
    @Test
    fun create_and_rename_do_not_preread_or_generate_local_records() = runTest {
        val requests = mutableListOf<GraphqlOperation>()
        val api = TagsApi(
            api = graphqlClient(
                query = { error("Unexpected preread: $it") },
                mutate = {
                    requests.add(it)
                    val field = if (it.query.contains("createTag(")) "createTag" else "updateTag"
                    JsonHelper.jsonDecode<JsonObject>("""{"$field":{"id":"server-tag","name":"Label","count":0,"type":"NOTE"}}""")
                },
            ),
        )
        assertEquals("server-tag", api.create(DataType.NOTE, "Label").id.also { assertEquals(1, requests.size) })
        assertTrue(requests.single().query.contains("createTag("))
        assertFalse("id" in requests.single().variables)
        requests.clear()
        assertEquals("server-tag", api.rename("server-tag", "New\n\"label\"").id)
        assertEquals(1, requests.size)
        assertEquals(JsonPrimitive("server-tag"), requests.single().variables["id"])
        assertEquals(JsonPrimitive("New\n\"label\""), requests.single().variables["name"])
        assertFalse("type" in requests.single().variables)
    }
    @Test
    fun batch_edits_send_one_key_list_without_metadata_or_prereads() = runTest {
        val requests = mutableListOf<GraphqlOperation>()
        val api = TagsApi(
            api = graphqlClient(
                query = { error("Unexpected preread") },
                mutate = {
                    requests.add(it)
                    JsonHelper.jsonDecode<JsonObject>("""{"updateTagRelations":true}""")
                },
            ),
        )
        api.edit(DataType.FILE, listOf("/a, b", "/second"), listOf("add"), listOf("remove"))
        assertEquals(1, requests.size)
        assertEquals(listOf("/a, b", "/second"), requests.single().variables.getValue("keys").jsonArray.map { it.jsonPrimitive.content })
        assertFalse("title" in requests.single().variables)
        assertFalse("size" in requests.single().variables)
        assertFalse("sizeBytes" in requests.single().variables)
        requests.clear()
        api.edit(DataType.FILE, emptyList(), listOf("add"), emptyList())
        api.edit(DataType.FILE, listOf("/a"), emptyList(), emptyList())
        assertTrue(requests.isEmpty())
    }
}
