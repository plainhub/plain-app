package com.ismartcoding.plain.lib.extensions

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ImageHeaderTest {

    // ── getFilenameFromPath (moved from fileNameFromPath) ─────────────────

    @Test fun `getFilenameFromPath returns last path segment`() {
        assertEquals("a.txt", "/tmp/a.txt".getFilenameFromPath())
        assertEquals("a.txt", "a.txt".getFilenameFromPath())
    }

    @Test fun `getFilenameFromPath is empty for path with no separator or trailing slash`() {
        assertEquals("", "".getFilenameFromPath())
        assertEquals("", "/".getFilenameFromPath())
        assertEquals("", "/tmp/".getFilenameFromPath())
    }

    // ── isHeifHeader ───────────────────────────────────────────────────────

    private fun ftypHeader(brand: String): ByteArray =
        byteArrayOf(0, 0, 0, 24, 'f'.code.toByte(), 't'.code.toByte(), 'y'.code.toByte(), 'p'.code.toByte()) +
            brand.encodeToByteArray()

    @Test fun `isHeifHeader accepts known HEIF and AVIF brands`() {
        for (brand in listOf("heic", "heix", "hevc", "hevx", "avif")) {
            assertTrue(isHeifHeader(ftypHeader(brand)), "brand $brand should be HEIF")
        }
    }

    @Test fun `isHeifHeader rejects non-heif ftyp brands`() {
        for (brand in listOf("jpeg", "mp42", "qt  ")) {
            assertFalse(isHeifHeader(ftypHeader(brand)), "brand $brand should not be HEIF")
        }
    }

    @Test fun `isHeifHeader rejects non-ftyp boxes and short headers`() {
        assertFalse(isHeifHeader("notftyp-heic".encodeToByteArray()))
        assertFalse(isHeifHeader("heic".encodeToByteArray()))
        assertFalse(isHeifHeader(byteArrayOf()))
    }

    @Test fun `isHeifHeader requires at least 12 bytes`() {
        val header = ftypHeader("heic")
        assertTrue(isHeifHeader(header))
        assertFalse(isHeifHeader(header.copyOfRange(0, 11)))
    }

    // ── isAnimatedImageOrSvgHeader ────────────────────────────────────────

    private fun ByteArray.asHeader(): ByteArray {
        val header = ByteArray(256)
        this.copyInto(header, 0, 0, minOf(size, 256))
        return header
    }

    @Test fun `svg by extension is animated-or-svg even without content`() {
        assertTrue(isAnimatedImageOrSvgHeader("icon.svg", ByteArray(256), 0L))
        assertTrue(isAnimatedImageOrSvgHeader("icon.svg", ByteArray(0), 0L))
    }

    @Test fun `png jpg jpeg extensions are never animated`() {
        assertFalse(isAnimatedImageOrSvgHeader("a.png", ByteArray(256), 100L))
        assertFalse(isAnimatedImageOrSvgHeader("a.jpg", ByteArray(256), 100L))
        assertFalse(isAnimatedImageOrSvgHeader("a.jpeg", ByteArray(256), 100L))
    }

    @Test fun `gif magic is animated`() {
        assertTrue(isAnimatedImageOrSvgHeader("anim.gif", "GIF89a...".encodeToByteArray().asHeader(), 100L))
        assertTrue(isAnimatedImageOrSvgHeader("anim.gif", "GIF87a...".encodeToByteArray().asHeader(), 100L))
    }

    @Test fun `animated webp vpx8 flag is detected`() {
        val header = "RIFF".encodeToByteArray() + ByteArray(4) + "WEBP".encodeToByteArray() +
            "VP8X".encodeToByteArray() + byteArrayOf(0b10)
        assertTrue(isAnimatedImageOrSvgHeader("anim.webp", header.asHeader(), 100L))
    }

    @Test fun `static webp is not animated`() {
        val header = "RIFF".encodeToByteArray() + ByteArray(4) + "WEBP".encodeToByteArray() +
            "VP8X".encodeToByteArray() + byteArrayOf(0)
        assertFalse(isAnimatedImageOrSvgHeader("static.webp", header.asHeader(), 100L))
    }

    @Test fun `animated heif brands detected while static heif is not`() {
        for (brand in listOf("msf1", "hevc", "hevx")) {
            assertTrue(isAnimatedImageOrSvgHeader("anim.heic", ftypHeader(brand).asHeader(), 100L), "brand $brand")
        }
        for (brand in listOf("heic", "avif")) {
            assertFalse(isAnimatedImageOrSvgHeader("static.heic", ftypHeader(brand).asHeader(), 100L), "brand $brand")
        }
    }

    @Test fun `svg content without extension is detected`() {
        val header = "<svg xmlns=\"http://www.w3.org/2000/svg\">".encodeToByteArray().asHeader()
        assertTrue(isAnimatedImageOrSvgHeader("unknown.bin", header, 200L))
    }

    @Test fun `jpeg and png magic bytes are not animated`() {
        assertFalse(isAnimatedImageOrSvgHeader("a.bin", byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()).asHeader(), 100L))
        assertFalse(isAnimatedImageOrSvgHeader("a.bin", byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47).asHeader(), 100L))
    }

    @Test fun `unknown content is neither animated nor svg`() {
        assertFalse(isAnimatedImageOrSvgHeader("a.bin", ByteArray(256), 100L))
    }
}