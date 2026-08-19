package com.ismartcoding.plain.platform

import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.db.MessageType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FilePlatformBuildersTest {

    // ── buildLongTextMessage ───────────────────────────────────────────────

    @Test fun `buildLongTextMessage wraps a single file message`() {
        val msg = buildLongTextMessage("/tmp/a.txt", "a.txt", "hello", 5L)
        assertEquals(MessageType.FILES, msg.type)
        val files = msg.value as com.ismartcoding.plain.db.DMessageFiles
        assertEquals(1, files.items.size)
        assertEquals("/tmp/a.txt", files.items[0].uri)
        assertEquals(5L, files.items[0].size)
        assertEquals("a.txt", files.items[0].fileName)
    }

    @Test fun `buildLongTextMessage keeps summary when text fits`() {
        val text = "short text"
        val msg = buildLongTextMessage("/tmp/a.txt", "a.txt", text, text.length.toLong())
        val files = msg.value as com.ismartcoding.plain.db.DMessageFiles
        assertEquals(text, files.items[0].summary)
    }

    @Test fun `buildLongTextMessage truncates overlong text to the summary cap`() {
        val text = "x".repeat(1000)
        val msg = buildLongTextMessage("/tmp/a.txt", "a.txt", text, text.length.toLong())
        val files = msg.value as com.ismartcoding.plain.db.DMessageFiles
        assertEquals(Constants.TEXT_FILE_SUMMARY_LENGTH, files.items[0].summary.length)
        assertEquals("x".repeat(Constants.TEXT_FILE_SUMMARY_LENGTH), files.items[0].summary)
    }

    // ── buildTextFile ──────────────────────────────────────────────────────

    @Test fun `buildTextFile fills DFile from path and size`() {
        val file = buildTextFile("/docs/notes.txt", 123L, 1_700_000_000_000L)
        assertEquals("notes.txt", file.name)
        assertEquals("/docs/notes.txt", file.path)
        assertEquals(123L, file.size)
        assertEquals(false, file.isDir)
        assertEquals(0, file.children)
        assertEquals("rw", file.permission)
        assertEquals("", file.mediaId)
        assertNull(file.createdAt)
        assertEquals(
            kotlin.time.Instant.fromEpochMilliseconds(1_700_000_000_000L),
            file.updatedAt,
        )
    }
}