package com.ismartcoding.plain.features.chat

import com.ismartcoding.plain.rust.graphql.graphqlClient
import com.ismartcoding.plain.models.ChatItem
import com.ismartcoding.plain.lib.JsonHelper
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.*
import kotlin.time.Instant

class ChatSnapshotClientTest {
    private fun chat(id: String) = ChatItem(
        id = id, fromId = "peer", toId = "me", content = com.ismartcoding.plain.models.ChatContent.Text("", emptyList()), status = ChatStatus.SENT,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"), updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
    )

    @Test
    fun batchesExistingChatItemSelectionsAndDeduplicatesIdsWithoutAnExtraApi() = runTest {
        val ids = (0 until 130).map { "message-$it" }
        val sizes = mutableListOf<Int>()
        val snapshot = readChatSnapshot(
            ids + ids.first(),
            api = graphqlClient(
                query = { selection ->
                    val size = selection.query.split("chatItem(").size - 1
                    sizes += size
                    JsonObject((0 until size).associate { "message$it" to JsonNull })
                },
            ),
        )
        assertEquals(listOf(128, 2), sizes)
        assertEquals(ids, snapshot.keys.toList())
        assertTrue(snapshot.values.all { it == null })
        assertTrue(readChatSnapshot(
            emptyList(),
            api = graphqlClient(
                query = { error("No request for empty UI state") },
            ),
        ).isEmpty())
    }

    @Test
    fun preservesLiteralIdsAndRequiresAnExplicitAliasResult() = runTest {
        val id = "quoted\"\nidentifier"
        val snapshot = readChatSnapshot(
            listOf(id),
            api = graphqlClient(
                query = { selection ->
                    assertEquals(id, selection.variables.getValue("id0").jsonPrimitive.content)
                    JsonObject(mapOf("message0" to JsonHelper.jsonEncodeToElement(chat(id))))
                },
            ),
        )
        assertEquals(id, snapshot.getValue(id)?.id)
        assertFailsWith<NoSuchElementException> { readChatSnapshot(
            listOf(id),
            api = graphqlClient(
                query = { JsonObject(emptyMap()) },
            ),
        ) }
        assertFailsWith<IllegalArgumentException> {
            readChatSnapshot(
                listOf(id),
                api = graphqlClient(
                    query = { JsonObject(mapOf("message0" to JsonHelper.jsonEncodeToElement(chat("other")))) },
                ),
            )
        }
    }
}
