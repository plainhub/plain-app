package com.ismartcoding.plain.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FilePlatformBuildersTest {

    // ── buildTextFile ──────────────────────────────────────────────────────

    @Test fun `buildTextFile fills DFile from path and size`() {
        val file = buildTextFile("/docs/notes.txt", 123L, 1_700_000_000_000L)
        assertEquals("notes.txt", file.name)
        assertEquals("/docs/notes.txt", file.path)
        assertEquals(123L, file.size)
        assertEquals(false, file.isDir)
        assertEquals(0, file.childCount)
        assertEquals("rw", file.permission)
        assertEquals("", file.mediaId)
        assertNull(file.createdAt)
        assertEquals(
            kotlin.time.Instant.fromEpochMilliseconds(1_700_000_000_000L),
            file.updatedAt,
        )
    }
}
