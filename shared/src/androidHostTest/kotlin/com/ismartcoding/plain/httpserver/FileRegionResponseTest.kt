package com.ismartcoding.plain.httpserver

import com.ismartcoding.plain.lib.ktorserver.PlainNettyServer
import com.ismartcoding.plain.lib.ktorserver.core.engine.EngineConnectorBuilder
import com.ismartcoding.plain.lib.ktorserver.core.engine.connector
import com.ismartcoding.plain.lib.ktorserver.core.http.content.FileRegionContent
import com.ismartcoding.plain.lib.ktorserver.core.plugins.mutableOriginConnectionPoint
import com.ismartcoding.plain.lib.ktorserver.core.request.path
import com.ismartcoding.plain.lib.ktorserver.core.response.respond
import com.ismartcoding.plain.lib.ktorserver.core.response.respondText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.Socket
import java.net.URI
import java.net.URL
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Boots the real Netty server through [PlainNettyServer] and verifies that
 * FileRegionContent responses arrive intact (status, headers, exact bytes)
 * for full-file and Range cases, and that HEAD requests keep keep-alive
 * connections usable.
 */
class FileRegionResponseTest {
    private val fileSize = 3L * 1024 * 1024
    private val mediaFileSize = 9L * 1024 * 1024
    private lateinit var file: File
    private lateinit var mediaFile: File
    private var port: Int = 0
    private lateinit var engine: PlainNettyServer

    @BeforeTest
    fun setUp() {
        file = File.createTempFile("fileregion-test", ".bin")
        writePattern(file, fileSize)
        mediaFile = File.createTempFile("fileregion-media-test", ".bin")
        writePattern(mediaFile, mediaFileSize)

        engine = PlainNettyServer(
            requestHandler = { call ->
                // Mirror production wiring: HEAD requests are rewritten to GET
                // for dispatch; the engine suppresses the response body.
                if (call.request.local.method == io.ktor.http.HttpMethod.Head) {
                    call.mutableOriginConnectionPoint.method = io.ktor.http.HttpMethod.Get
                }
                when (call.request.path()) {
                    "/file" -> call.respond(
                        FileRegionContent(
                            file = file,
                            offset = 0,
                            length = fileSize,
                            contentType = ContentType.Application.OctetStream,
                            status = HttpStatusCode.OK,
                            headers = headersOf("Accept-Ranges" to listOf("bytes")),
                        )
                    )
                    "/range" -> call.respond(
                        FileRegionContent(
                            file = file,
                            offset = 1000,
                            length = 4096,
                            contentType = ContentType.Application.OctetStream,
                            status = HttpStatusCode.PartialContent,
                            headers = headersOf(
                                "Accept-Ranges" to listOf("bytes"),
                                "Content-Range" to listOf("bytes 1000-5095/$fileSize"),
                            ),
                        )
                    )
                    "/media" -> {
                        val range = resolveSingleByteRange(call.request.headers["Range"], mediaFileSize)
                            ?.let { capBrowserMediaRange(it, call.request.headers["Sec-Fetch-Dest"]) }
                        if (range != null) {
                            call.respond(
                                FileRegionContent(
                                    file = mediaFile,
                                    offset = range.start,
                                    length = range.length,
                                    contentType = ContentType.Video.MP4,
                                    status = if (range.isPartial) HttpStatusCode.PartialContent else HttpStatusCode.OK,
                                    headers = fileRangeHeaders(range, mediaFileSize),
                                )
                            )
                        }
                    }
                    "/text" -> call.respondText("hello-region", ContentType.Text.Plain)
                }
            },
        ) {
            connector {
                port = 0
                host = "127.0.0.1"
            }
        }
        engine.start(wait = false)
        port = runBlocking { engine.resolvedConnectors().first().port }
    }

    @AfterTest
    fun tearDown() {
        engine.stop(50, 500)
        file.delete()
        mediaFile.delete()
    }

    private fun writePattern(target: File, size: Long) {
        target.outputStream().use { out ->
            val buffer = ByteArray(64 * 1024)
            var written = 0L
            while (written < size) {
                val len = minOf(buffer.size.toLong(), size - written).toInt()
                for (i in 0 until len) buffer[i] = ((written + i) % 251).toByte()
                out.write(buffer, 0, len)
                written += len
            }
        }
    }

    @Test
    fun fullFileResponse_matchesBytes() {
        val connection = get("/file")
        assertEquals(200, connection.responseCode)
        assertEquals(fileSize.toString(), connection.getHeaderField("Content-Length"))
        val body = connection.inputStream.use { it.readBytes() }
        assertEquals(fileSize.toInt(), body.size)
        val expected = file.inputStream().use { it.readBytes() }
        assertTrue(expected.contentEquals(body), "full file body mismatch")
    }

    @Test
    fun rangeResponse_returnsExactSlice() {
        val connection = get("/range")
        assertEquals(206, connection.responseCode)
        assertEquals("bytes 1000-5095/$fileSize", connection.getHeaderField("Content-Range"))
        assertEquals("4096", connection.getHeaderField("Content-Length"))
        val body = connection.inputStream.use { it.readBytes() }
        assertEquals(4096, body.size)
        val expected = readPattern(file, 1000, 4096)
        assertTrue(expected.contentEquals(body), "range body mismatch")
    }

    @Test
    fun respondText_returnsBodyAndContentType() {
        val connection = get("/text")
        assertEquals(200, connection.responseCode)
        val body = connection.inputStream.use { it.readBytes().decodeToString() }
        assertEquals("hello-region", body)
    }

    @Test
    fun mediaElement_openEndedRange_isCappedTo4MiB() {
        // java.net.http.HttpClient is used because HttpURLConnection silently
        // drops Sec-Fetch-Dest, which real browsers always send.
        val response = java.net.http.HttpClient.newHttpClient().send(
            java.net.http.HttpRequest.newBuilder(URI.create("http://127.0.0.1:$port/media"))
                .header("Range", "bytes=0-")
                .header("Sec-Fetch-Dest", "video")
                .GET().build(),
            java.net.http.HttpResponse.BodyHandlers.ofByteArray(),
        )
        assertEquals(206, response.statusCode())
        assertEquals("bytes 0-${BROWSER_MEDIA_RANGE_BYTES - 1}/$mediaFileSize", response.headers().firstValue("Content-Range").orElse(null))
        val body = response.body()
        assertEquals(BROWSER_MEDIA_RANGE_BYTES.toInt(), body.size)
        val expected = readPattern(mediaFile, 0, BROWSER_MEDIA_RANGE_BYTES.toInt())
        assertTrue(expected.contentEquals(body), "capped media body mismatch")
    }

    @Test
    fun nonMediaElement_openEndedRange_returnsWholeFile() {
        val response = java.net.http.HttpClient.newHttpClient().send(
            java.net.http.HttpRequest.newBuilder(URI.create("http://127.0.0.1:$port/media"))
                .header("Range", "bytes=0-")
                .header("Sec-Fetch-Dest", "document")
                .GET().build(),
            java.net.http.HttpResponse.BodyHandlers.ofByteArray(),
        )
        assertEquals(206, response.statusCode())
        assertEquals("bytes 0-${mediaFileSize - 1}/$mediaFileSize", response.headers().firstValue("Content-Range").orElse(null))
        assertEquals(mediaFileSize.toInt(), response.body().size)
    }

    @Test
    fun headRequest_returnsHeadersOnly() {
        val connection = get("/file").apply { requestMethod = "HEAD" }
        assertEquals(200, connection.responseCode)
        assertEquals(fileSize.toString(), connection.getHeaderField("Content-Length"))
        val body = connection.inputStream.use { it.readBytes() }
        assertEquals(0, body.size)
    }

    @Test
    fun headRequest_keepAliveConnectionStaysUsable() {
        Socket("127.0.0.1", port).use { socket ->
            val output = socket.getOutputStream()
            val input = socket.getInputStream()

            output.write("HEAD /file HTTP/1.1\r\nHost: 127.0.0.1\r\n\r\n".toByteArray())
            output.flush()
            val head = readRawResponse(input, readBody = false)
            assertTrue(head.statusLine.contains(" 200 "), "unexpected status: ${head.statusLine}")
            assertEquals(fileSize.toString(), head.headers["content-length"])

            // A HEAD response must carry no body bytes; the next bytes on this
            // socket have to be the response to the following request.
            output.write("GET /text HTTP/1.1\r\nHost: 127.0.0.1\r\nConnection: close\r\n\r\n".toByteArray())
            output.flush()
            val next = readRawResponse(input, readBody = true)
            assertTrue(next.statusLine.contains(" 200 "), "connection corrupted after HEAD: ${next.statusLine}")
            assertEquals("hello-region", next.body.decodeToString())
        }
    }

    private class RawResponse(val statusLine: String, val headers: Map<String, String>, val body: ByteArray)

    private fun readRawResponse(input: InputStream, readBody: Boolean): RawResponse {
        val head = StringBuilder()
        val one = ByteArray(1)
        while (!head.endsWith("\r\n\r\n")) {
            val n = input.read(one)
            if (n <= 0) break
            head.append(one[0].toInt().toChar())
        }
        val lines = head.toString().trim().split("\r\n")
        val headers = lines.drop(1).mapNotNull {
            val idx = it.indexOf(':')
            if (idx > 0) it.substring(0, idx).trim().lowercase() to it.substring(idx + 1).trim() else null
        }.toMap()
        val length = headers["content-length"]?.toIntOrNull() ?: 0
        val body = ByteArray(if (readBody) length else 0)
        var read = 0
        while (read < body.size) {
            val n = input.read(body, read, body.size - read)
            if (n <= 0) break
            read += n
        }
        return RawResponse(lines.first(), headers, body)
    }

    private fun readPattern(source: File, offset: Long, length: Int): ByteArray {
        val expected = ByteArray(length)
        source.inputStream().use { input ->
            input.channel.position(offset)
            var read = 0
            while (read < length) {
                val n = input.read(expected, read, length - read)
                if (n <= 0) break
                read += n
            }
        }
        return expected
    }

    private fun get(path: String, headers: Map<String, String> = emptyMap()): HttpURLConnection {
        val connection = URL("http://127.0.0.1:$port$path").openConnection() as HttpURLConnection
        headers.forEach { (k, v) -> connection.setRequestProperty(k, v) }
        return connection
    }
}
