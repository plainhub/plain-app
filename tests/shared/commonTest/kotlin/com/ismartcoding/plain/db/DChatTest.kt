package com.ismartcoding.plain.db

import com.ismartcoding.plain.features.chat.models.MessageFile
import com.ismartcoding.plain.models.ChatContent
import com.ismartcoding.plain.models.ChatContentInput
import com.ismartcoding.plain.models.ChatMessageInput
import com.ismartcoding.plain.features.chat.models.ChatTextFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.encodeToJsonElement
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json

class DChatTest {
    @Test
    fun `decodes each concrete GraphQL content type`() {
        val value = com.ismartcoding.plain.lib.JsonHelper.jsonDecode<ChatContent>("""{"__typename":"ChatMessageContent","text":"hello","format":"PLAIN","items":[],"linkPreviews":[]}""")
        assertEquals("hello", (value as ChatContent.Message).text)
        val share = com.ismartcoding.plain.lib.JsonHelper.jsonDecode<ChatContent>("""{"__typename":"ChatShareContent","shareId":"share","urlToken":"","peerInfo":{"id":"p","ip":"127.0.0.1","port":443},"name":"Folder","itemCount":1,"totalSize":2,"expiresAt":null}""")
        assertEquals("Folder", (share as ChatContent.Share).name)
    }
    @Test
    fun `rejects unknown and serialized string content`() {
        for (content in listOf("""{"__typename":"UnknownContent"}""", """{"__typename":"ChatTextContent","text":"legacy"}""", """"raw text"""")) {
            kotlin.test.assertFails { com.ismartcoding.plain.lib.JsonHelper.jsonDecode<com.ismartcoding.plain.models.ChatContent>(content) }
        }
    }
    @Test
    fun `oneOf inputs encode only the selected field`() {
        val input = ChatContentInput(message = ChatMessageInput("hello", ChatTextFormat.PLAIN, emptyList()))
        val encoded = com.ismartcoding.plain.lib.JsonHelper.jsonEncodeToElement(input) as kotlinx.serialization.json.JsonObject
        assertEquals(setOf("message"), encoded.keys)
        kotlin.test.assertFailsWith<IllegalArgumentException> { ChatContentInput() }
    }
}

class DMessageFileDurationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `decodes durationMs natively`() {
        val files = json.decodeFromString<MessageFile>("""{"id":"1","uri":"fid:a","size":9,"durationMs":30000}""")
        assertEquals(30000L, files.durationMs)
    }

    @Test
    fun `decodes legacy duration as milliseconds`() {
        val files = json.decodeFromString<MessageFile>("""{"id":"1","uri":"fid:a","size":9,"duration":30}""")
        assertEquals(30000L, files.durationMs)
    }

    @Test
    fun `missing duration decodes as zero`() {
        val files = json.decodeFromString<MessageFile>("""{"id":"1","uri":"fid:a","size":9}""")
        assertEquals(0L, files.durationMs)
    }

    @Test
    fun `encodes durationMs and drops legacy keys`() {
        val item = MessageFile(id = "1", uri = "fid:a", size = 9, durationMs = 30000)
        val obj = json.encodeToString(item)
        assertTrue(obj.contains(""""durationMs":30000"""), obj)
        assertFalse(obj.contains(""""duration""""), obj)
    }
}
