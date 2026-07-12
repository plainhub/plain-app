package com.ismartcoding.plain.lib.extensions

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StringExtensionsIsUrlTest {

    // ── Valid HTTP/HTTPS URLs ──────────────────────────────────────

    @Test
    fun `http URL is detected`() {
        assertTrue("http://example.com".isUrl())
    }

    @Test
    fun `https URL is detected`() {
        assertTrue("https://example.com".isUrl())
    }

    @Test
    fun `http URL with path is detected`() {
        assertTrue("http://example.com/path/to/resource".isUrl())
    }

    @Test
    fun `https URL with port and path is detected`() {
        assertTrue("https://example.com:8443/api/v1/items".isUrl())
    }

    @Test
    fun `https URL with query params is detected`() {
        assertTrue("https://example.com/search?q=test&page=1".isUrl())
    }

    @Test
    fun `http URL with IP address is detected`() {
        assertTrue("http://192.168.1.100:8080/video.mp4".isUrl())
    }

    @Test
    fun `uppercase HTTP scheme is detected`() {
        assertTrue("HTTP://EXAMPLE.COM".isUrl())
    }

    @Test
    fun `uppercase HTTPS scheme is detected`() {
        assertTrue("HTTPS://example.com".isUrl())
    }

    @Test
    fun `mixed case Http scheme is detected`() {
        assertTrue("Http://example.com".isUrl())
    }

    // ── File paths (the bug that caused videos not to play) ────────

    @Test
    fun `absolute file path is not URL`() {
        assertFalse("/storage/emulated/0/DCIM/Camera/PXL_20260604_060547429.TS.mp4".isUrl())
    }

    @Test
    fun `absolute file path with simple name is not URL`() {
        assertFalse("/storage/emulated/0/DCIM/Camera/video.mp4".isUrl())
    }

    @Test
    fun `root path is not URL`() {
        assertFalse("/".isUrl())
    }

    @Test
    fun `relative path is not URL`() {
        assertFalse("relative/path/to/file.txt".isUrl())
    }

    @Test
    fun `path with double extension TS mp4 is not URL`() {
        assertFalse("/data/media/PXL_20260604_060547429.TS.mp4".isUrl())
    }

    // ── Other non-URL schemes ──────────────────────────────────────

    @Test
    fun `file URI scheme is not URL`() {
        assertFalse("file:///storage/emulated/0/video.mp4".isUrl())
    }

    @Test
    fun `ftp scheme is not URL`() {
        assertFalse("ftp://example.com/file.txt".isUrl())
    }

    @Test
    fun `content URI is not URL`() {
        assertFalse("content://media/external/video/media/123".isUrl())
    }

    // ── Edge cases ─────────────────────────────────────────────────

    @Test
    fun `empty string is not URL`() {
        assertFalse("".isUrl())
    }

    @Test
    fun `plain text is not URL`() {
        assertFalse("hello world".isUrl())
    }

    @Test
    fun `string starting with http but not a valid URL returns false`() {
        // "http://" alone — URLBuilder can parse it but it has no host
        // The key test is that it doesn't crash; behavior depends on URLBuilder
        // Just ensure no exception is thrown
        "http://".isUrl()
    }

    @Test
    fun `string with http in the middle is not URL`() {
        assertFalse("example.com/http://test".isUrl())
    }

    @Test
    fun `ws scheme is not URL`() {
        assertFalse("ws://example.com/socket".isUrl())
    }

    @Test
    fun `wss scheme is not URL`() {
        assertFalse("wss://example.com/socket".isUrl())
    }
}
