package com.ismartcoding.plain.platform

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ContentTypeTest {

    private fun tempFile(name: String): File {
        val dir = createTempDir()
        dir.deleteOnExit()
        return File(dir, name).apply {
            writeText("test")
            deleteOnExit()
        }
    }

    private fun createTempDir(): File =
        java.nio.file.Files.createTempDirectory("contentTypeTest").toFile().apply { deleteOnExit() }

    // ── Existing files ───

    @Test
    fun `resolves mime from file extension`() {
        assertEquals("application/pdf", getContentTypeForPath(tempFile("report.pdf").absolutePath))
    }

    @Test
    fun `resolves image mime`() {
        assertEquals("image/png", getContentTypeForPath(tempFile("photo.png").absolutePath))
    }

    @Test
    fun `resolves text mime`() {
        assertEquals("text/plain", getContentTypeForPath(tempFile("notes.txt").absolutePath))
    }

    @Test
    fun `handles uppercase extension`() {
        assertEquals("application/pdf", getContentTypeForPath(tempFile("report.PDF").absolutePath))
    }

    // ── Missing / ambiguous paths ───

    @Test
    fun `returns null for nonexistent path`() {
        assertNull(getContentTypeForPath("/nonexistent/missing.pdf"))
    }

    @Test
    fun `returns wildcard for extension-less file`() {
        assertEquals("*/*", getContentTypeForPath(tempFile("noextension").absolutePath))
    }
}