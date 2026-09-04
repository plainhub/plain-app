package com.ismartcoding.plain.httpserver

import com.ismartcoding.plain.lib.ktorserver.Netty
import com.ismartcoding.plain.lib.ktorserver.NettyApplicationEngine
import com.ismartcoding.plain.lib.ktorserver.core.engine.EmbeddedServer
import com.ismartcoding.plain.lib.ktorserver.core.engine.embeddedServer
import com.ismartcoding.plain.lib.ktorserver.core.http.content.FileRegionContent
import com.ismartcoding.plain.lib.ktorserver.core.response.respond
import com.ismartcoding.plain.lib.ktorserver.core.response.respondText
import com.ismartcoding.plain.lib.ktorserver.core.routing.routing
import com.ismartcoding.plain.lib.ktorserver.core.routing.get
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Boots the real Netty engine and verifies that FileRegionContent responses
 * arrive intact (status, headers, exact bytes) for full-file and Range cases.
 */
class FileRegionResponseTest {
    private val fileSize = 3L * 1024 * 1024
    private val mediaFileSize = 9L * 1024 * 1024
    private lateinit var file: File
    private lateinit var mediaFile: File
    private var port: Int = 0
    private lateinit var engine: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>

    @BeforeTest
    fun setUp() {
        file = File.createTempFile("fileregion-test", ".bin")
        writePattern(file, fileSize)
        mediaFile = File.createTempFile("fileregion-media-test", ".bin")
        writePattern(mediaFile, mediaFileSize)

        engine = embeddedServer(Netty, port = 0, host = "127.0.0.1") {
            routing {
                get("/file") {
                    call.respond(
                        FileRegionContent(
                            file = file,
                            offset = 0,
                            length = fileSize,
                            contentType = ContentType.Application.OctetStream,
                            status = HttpStatusCode.OK,
                            headers = headersOf("Accept-Ranges" to listOf("bytes")),
                        )
                    )
                }
                get("/range") {
                    call.respond(
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
                }
                get("/media") {
                    val range = resolveSingleByteRange(call.request.headers["Range"], mediaFileSize)
                        ?.let { capBrowserMediaRange(it, call.request.headers["Sec-Fetch-Dest"]) }
                        ?: return@get
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
                get("/text") {
                    call.respondText("hello-region", ContentType.Text.Plain)
                }
            }
        }
        engine.start(wait = false)
        port = runBlocking { engine.engine.resolvedConnectors().first().port }
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
        val expected = ByteArray(4096)
        file.inputStream().use { input ->
            input.channel.position(1000)
            var read = 0
            while (read < expected.size) {
                val n = input.read(expected, read, expected.size - read)
                if (n <= 0) break
                read += n
            }
        }
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
