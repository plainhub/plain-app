package com.ismartcoding.plain.httpserver

import com.ismartcoding.plain.lib.ktorserver.ConditionalHeaders
import com.ismartcoding.plain.lib.ktorserver.Netty
import com.ismartcoding.plain.lib.ktorserver.core.application.install
import com.ismartcoding.plain.lib.ktorserver.core.engine.EmbeddedServer
import com.ismartcoding.plain.lib.ktorserver.core.engine.applicationEnvironment
import com.ismartcoding.plain.lib.ktorserver.core.engine.connector
import com.ismartcoding.plain.lib.ktorserver.core.engine.embeddedServer
import com.ismartcoding.plain.lib.ktorserver.core.response.header
import com.ismartcoding.plain.lib.ktorserver.core.response.respondBytes
import com.ismartcoding.plain.lib.ktorserver.core.routing.get
import com.ismartcoding.plain.lib.ktorserver.core.routing.routing
import io.ktor.http.ContentType
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.net.HttpURLConnection
import java.net.URL
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Verifies the thumbnail caching mechanism end to end: an ETag set via
 * response headers is picked up by the ConditionalHeaders plugin, and a
 * matching If-None-Match revalidation is answered with 304 and no body —
 * without regenerating/sending the thumbnail bytes.
 */
class ThumbnailETagTest {
    private var port: Int = 0
    private lateinit var engine: EmbeddedServer<*, *>
    private var generations = 0

    @BeforeTest
    fun setUp() {
        generations = 0
        engine = embeddedServer(
            Netty,
            applicationEnvironment { log = LoggerFactory.getLogger("thumb-etag-test") },
            configure = {
                connector {
                    port = 0
                    host = "127.0.0.1"
                }
            },
            module = {
                install(ConditionalHeaders)
                routing {
                    get("/fs") {
                        // Mirrors FileServer's thumbnail branch: validators go
                        // through response headers, the body through respondBytes.
                        generations++
                        call.response.header("Cache-Control", "private, max-age=86400")
                        call.response.header("ETag", "\"thumb-1-2-96-96-3\"")
                        call.respondBytes(ByteArray(1024) { it.toByte() }, ContentType.Image.JPEG)
                    }
                }
            },
        )
        engine.start(wait = false)
        port = runBlocking { engine.engine.resolvedConnectors().first().port }
    }

    @AfterTest
    fun tearDown() {
        engine.stop(50, 500)
    }

    private fun get(etag: String? = null): HttpURLConnection {
        val connection = URL("http://127.0.0.1:$port/fs").openConnection() as HttpURLConnection
        etag?.let { connection.setRequestProperty("If-None-Match", it) }
        return connection
    }

    @Test
    fun firstRequest_returns200WithETag() {
        val connection = get()
        assertEquals(200, connection.responseCode)
        assertEquals("\"thumb-1-2-96-96-3\"", connection.getHeaderField("ETag"))
        assertEquals("private, max-age=86400", connection.getHeaderField("Cache-Control"))
        val body = connection.inputStream.use { it.readBytes() }
        assertEquals(1024, body.size)
        assertEquals(1, generations)
    }

    @Test
    fun revalidation_withMatchingETag_returns304WithoutBody() {
        val etag = get().getHeaderField("ETag")
        val connection = get(etag)
        assertEquals(304, connection.responseCode)
        val body = connection.errorStream?.use { it.readBytes() } ?: ByteArray(0)
        assertEquals(0, body.size, "304 must not carry a body")
        assertEquals(2, generations, "304 path must not be taken as a fresh generation")
    }

    @Test
    fun revalidation_withStaleETag_returns200() {
        val connection = get("\"thumb-9-9-9-9-9\"")
        assertEquals(200, connection.responseCode)
        assertTrue(connection.inputStream.use { it.readBytes() }.isNotEmpty())
    }
}
