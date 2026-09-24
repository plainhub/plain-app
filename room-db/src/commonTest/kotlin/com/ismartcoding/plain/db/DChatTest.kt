package com.ismartcoding.plain.db

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.encodeToJsonElement

class DChatTest {

    @Test
    fun `parses TEXT content`() {
        val message = DChat.parseContent("""{"type":"TEXT","value":{"text":"hello","linkPreviews":[]}}""")
        assertEquals(MessageType.TEXT, message.type)
        assertEquals("hello", (message.value as DMessageText).text)
    }

    @Test
    fun `parses IMAGES content`() {
        val message = DChat.parseContent("""{"type":"IMAGES","value":{"items":[{"id":"1","uri":"fid:abc","size":0}]}}""")
        assertEquals(MessageType.IMAGES, message.type)
        assertEquals(1, (message.value as DMessageImages).items.size)
    }

    @Test
    fun `parses FILES content`() {
        val message = DChat.parseContent("""{"type":"FILES","value":{"items":[{"id":"1","uri":"fsid:def","size":0}]}}""")
        assertEquals(MessageType.FILES, message.type)
        assertEquals(1, (message.value as DMessageFiles).items.size)
    }

    // Regression: unknown type previously crashed with IllegalArgumentException
    @Test
    fun `falls back to TEXT for unknown type`() {
        val message = DChat.parseContent("""{"type":"VIDEO","value":{"text":"hi"}}""")
        assertEquals(MessageType.TEXT, message.type)
        assertEquals("hi", (message.value as DMessageText).text)
    }

    @Test
    fun `falls back to TEXT for missing type`() {
        val message = DChat.parseContent("""{"value":{"text":"hi"}}""")
        assertEquals(MessageType.TEXT, message.type)
        assertEquals("hi", (message.value as DMessageText).text)
    }

    @Test
    fun `shows raw content with type when value cannot be decoded`() {
        val message = DChat.parseContent("""{"type":"TEXT","value":{"items":[]}}""")
        assertEquals(MessageType.TEXT, message.type)
        assertEquals("""{"type":"TEXT","value":{"items":[]}}""", (message.value as DMessageText).text)
    }

    @Test
    fun `shows raw content with unknown type when value cannot be decoded`() {
        val message = DChat.parseContent("""{"type":"VIDEO","value":{"url":"a.mp4"}}""")
        assertEquals(MessageType.TEXT, message.type)
        assertEquals("""{"type":"VIDEO","value":{"url":"a.mp4"}}""", (message.value as DMessageText).text)
    }

    @Test
    fun `shows raw content when value is missing`() {
        val message = DChat.parseContent("""{"type":"TEXT"}""")
        assertEquals(MessageType.TEXT, message.type)
        assertEquals("""{"type":"TEXT"}""", (message.value as DMessageText).text)
    }
}

class DMessageFileDurationTest {

    private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

    @Test
    fun `decodes durationMs natively`() {
        val files = json.decodeFromString<DMessageFiles>("""{"items":[{"id":"1","uri":"fid:a","size":9,"durationMs":30000}]}""")
        assertEquals(30000L, files.items[0].durationMs)
    }

    @Test
    fun `decodes legacy durationSec as milliseconds`() {
        val files = json.decodeFromString<DMessageFiles>("""{"items":[{"id":"1","uri":"fid:a","size":9,"durationSec":30}]}""")
        assertEquals(30000L, files.items[0].durationMs)
    }

    @Test
    fun `decodes legacy duration as milliseconds`() {
        val files = json.decodeFromString<DMessageFiles>("""{"items":[{"id":"1","uri":"fid:a","size":9,"duration":30}]}""")
        assertEquals(30000L, files.items[0].durationMs)
    }

    @Test
    fun `missing duration decodes as zero`() {
        val files = json.decodeFromString<DMessageFiles>("""{"items":[{"id":"1","uri":"fid:a","size":9}]}""")
        assertEquals(0L, files.items[0].durationMs)
    }

    @Test
    fun `encodes durationMs and drops legacy keys`() {
        val item = DMessageFile(id = "1", uri = "fid:a", size = 9, durationMs = 30000)
        val obj = json.encodeToString(DMessageFiles(listOf(item)))
        kotlin.test.assertTrue(obj.contains(""""durationMs":30000"""), obj)
        kotlin.test.assertFalse(obj.contains("durationSec"), obj)
        kotlin.test.assertFalse(obj.contains(""""duration""""), obj)
    }
}
