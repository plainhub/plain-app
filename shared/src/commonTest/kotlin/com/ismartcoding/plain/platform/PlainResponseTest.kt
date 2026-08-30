package com.ismartcoding.plain.platform

import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlainResponseTest {

    private fun response(
        status: Int = 200,
        url: String = "https://example.com",
        headers: Map<String, List<String>> = emptyMap(),
        body: ByteArray = ByteArray(0),
    ) = PlainResponse(
        status = HttpStatusCode(status),
        url = url,
        headers = headers,
        channel = ByteReadChannel(body),
    )

    // ── header() lookup ───

    @Test
    fun `header finds exact-case key`() {
        val r = response(headers = mapOf("Content-Type" to listOf("text/html")))
        assertEquals("text/html", r.header("Content-Type"))
    }

    @Test
    fun `header matches case-insensitively`() {
        val r = response(headers = mapOf("content-type" to listOf("text/html")))
        assertEquals("text/html", r.header("Content-Type"))
    }

    @Test
    fun `header matches mixed-case key with canonical query`() {
        val r = response(headers = mapOf("Content-Type" to listOf("image/png")))
        assertEquals("image/png", r.header("content-type"))
    }

    @Test
    fun `header returns null for missing key`() {
        val r = response(headers = mapOf("Content-Type" to listOf("text/html")))
        assertNull(r.header("Content-Length"))
    }

    @Test
    fun `header returns first value of multi-value header`() {
        val r = response(headers = mapOf("Set-Cookie" to listOf("a=1", "b=2")))
        assertEquals("a=1", r.header("set-cookie"))
    }

    // ── status semantics ───

    @Test
    fun `isOk is true only for 200`() {
        assertTrue(response(status = 200).isOk())
        assertFalse(response(status = 204).isOk())
    }

    @Test
    fun `isSuccess is true for any 2xx`() {
        assertTrue(response(status = 204).isSuccess())
        assertFalse(response(status = 301).isSuccess())
        assertFalse(response(status = 404).isSuccess())
    }

    // ── toString / body ───

    @Test
    fun `toString includes status and url`() {
        assertEquals("200 OK https://example.com", response().toString())
    }

    @Test
    fun `bodyAsText decodes the streamed body`() {
        assertEquals("hello", runBlocking { response(body = "hello".encodeToByteArray()).bodyAsText() })
    }

    // ── close ───

    @Test
    fun `close invokes onClose`() {
        var closed = 0
        val r = PlainResponse(HttpStatusCode(200), "https://example.com", emptyMap(), ByteReadChannel(ByteArray(0))) { closed++ }
        r.close()
        assertEquals(1, closed)
    }
}
