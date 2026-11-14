package com.ismartcoding.plain.db

import com.ismartcoding.plain.features.chat.models.MessageFile
import com.ismartcoding.plain.features.chat.models.MessageFiles
import com.ismartcoding.plain.features.chat.models.MessageImages
import com.ismartcoding.plain.features.chat.models.MessageText
import com.ismartcoding.plain.features.chat.models.MessageType
import com.ismartcoding.plain.models.ChatItem
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
        val text = com.ismartcoding.plain.lib.JsonHelper.jsonDecode<com.ismartcoding.plain.models.ChatContent>("""{"__typename":"ChatTextContent","text":"hello","linkPreviews":[]}""")
        assertEquals("hello", (text as com.ismartcoding.plain.models.ChatContent.Text).text)
        for (kind in listOf("ChatImagesContent", "ChatFilesContent")) {
            val content = com.ismartcoding.plain.lib.JsonHelper.jsonDecode<com.ismartcoding.plain.models.ChatContent>("""{"__typename":"$kind","items":[]}""")
            assertTrue(content is com.ismartcoding.plain.models.ChatContent.Images || content is com.ismartcoding.plain.models.ChatContent.Files)
        }
    }
    @Test
    fun `rejects unknown and serialized string content`() {
        for (content in listOf("""{"__typename":"UnknownContent"}""", """"raw text"""")) {
            kotlin.test.assertFails { com.ismartcoding.plain.lib.JsonHelper.jsonDecode<com.ismartcoding.plain.models.ChatContent>(content) }
        }
    }
    @Test
    fun `oneOf inputs encode only the selected field`() {
        val input = com.ismartcoding.plain.models.ChatContentInput(text = MessageText("hello"))
        val encoded = com.ismartcoding.plain.lib.JsonHelper.jsonEncodeToElement(input) as kotlinx.serialization.json.JsonObject
        assertEquals(setOf("text"), encoded.keys)
        kotlin.test.assertFailsWith<IllegalArgumentException> { com.ismartcoding.plain.models.ChatContentInput() }
        kotlin.test.assertFailsWith<IllegalArgumentException> { com.ismartcoding.plain.models.ChatContentInput(text = MessageText("hello"), files = MessageFiles(emptyList())) }
    }
}

class DMessageFileDurationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `decodes durationMs natively`() {
        val files = json.decodeFromString<MessageFiles>("""{"items":[{"id":"1","uri":"fid:a","size":9,"durationMs":30000}]}""")
        assertEquals(30000L, files.items[0].durationMs)
    }

    @Test
    fun `decodes legacy duration as milliseconds`() {
        val files = json.decodeFromString<MessageFiles>("""{"items":[{"id":"1","uri":"fid:a","size":9,"duration":30}]}""")
        assertEquals(30000L, files.items[0].durationMs)
    }

    @Test
    fun `missing duration decodes as zero`() {
        val files = json.decodeFromString<MessageFiles>("""{"items":[{"id":"1","uri":"fid:a","size":9}]}""")
        assertEquals(0L, files.items[0].durationMs)
    }

    @Test
    fun `encodes durationMs and drops legacy keys`() {
        val item = MessageFile(id = "1", uri = "fid:a", size = 9, durationMs = 30000)
        val obj = json.encodeToString(MessageFiles(listOf(item)))
        assertTrue(obj.contains(""""durationMs":30000"""), obj)
        assertFalse(obj.contains(""""duration""""), obj)
    }
}
