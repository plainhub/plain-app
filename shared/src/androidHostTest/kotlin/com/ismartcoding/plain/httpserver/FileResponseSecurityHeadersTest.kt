package com.ismartcoding.plain.httpserver

import com.ismartcoding.plain.lib.ktorserver.Netty
import com.ismartcoding.plain.lib.ktorserver.NettyApplicationEngine
import com.ismartcoding.plain.lib.ktorserver.core.engine.EmbeddedServer
import com.ismartcoding.plain.lib.ktorserver.core.engine.applicationEnvironment
import com.ismartcoding.plain.lib.ktorserver.core.engine.connector
import com.ismartcoding.plain.lib.ktorserver.core.engine.embeddedServer
import com.ismartcoding.plain.lib.ktorserver.core.routing.get
import com.ismartcoding.plain.lib.ktorserver.core.routing.routing
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Locks the stored-XSS mitigation for user files served through the /fs
 * family: document types a browser executes scripts in when navigated to
 * (SVG/HTML/XHTML) must go out with `Content-Security-Policy: sandbox`
 * (opaque origin, scripts and forms dead, visual preview intact) plus
 * `X-Content-Type-Options: nosniff`. Inert types get nosniff only, so normal
 * media embedding is untouched. Exercises the real KtorHttpCall.respondFile
 * path behind FileServer.serve against a real Netty engine.
 */
class FileResponseSecurityHeadersTest {
    private var engine: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? = null
    private var port: Int = 0

    @AfterTest
    fun tearDown() {
        engine?.stop(0, 500)
        engine = null
    }

    @Test
    fun svgFile_getsSandboxCsp_andNosniff() {
        val svg = tempFile("payload.svg", "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(1)</script></svg>")
        serveFiles(svg)
        val response = get("/fs")

        assertEquals(200, response.statusCode())
        assertTrue(
            response.headers().firstValue("Content-Type").orElse("").contains("image/svg+xml"),
            "SVG must keep its preview content type, got ${response.headers().firstValue("Content-Type").orElse("")}",
        )
        assertEquals("sandbox", response.headers().firstValue("Content-Security-Policy").orElse(""))
        assertEquals("nosniff", response.headers().firstValue("X-Content-Type-Options").orElse(""))
    }

    @Test
    fun htmlFile_getsSandboxCsp_andNosniff() {
        val html = tempFile("page.html", "<html><body><script>alert(1)</script></body></html>")
        serveFiles(html)
        val response = get("/fs")

        assertEquals(200, response.statusCode())
        assertEquals("sandbox", response.headers().firstValue("Content-Security-Policy").orElse(""))
        assertEquals("nosniff", response.headers().firstValue("X-Content-Type-Options").orElse(""))
    }

    @Test
    fun inertBinary_getsNosniff_butNoCsp() {
        val bin = tempFile("blob.bin", ByteArray(64) { it.toByte() })
        serveFiles(bin)
        val response = get("/fs")

        assertEquals(200, response.statusCode())
        assertEquals("nosniff", response.headers().firstValue("X-Content-Type-Options").orElse(""))
        assertTrue(response.headers().firstValue("Content-Security-Policy").isEmpty, "CSP sandbox must not be attached to inert media types")
    }

    private fun serveFiles(file: File) {
        val server = embeddedServer(
            Netty,
            applicationEnvironment { log = LoggerFactory.getLogger("file-security-headers-test") },
            configure = {
                connector {
                    port = 0
                    host = "127.0.0.1"
                }
            },
            module = {
                routing {
                    get("/fs") {
                        FileServer.serve(KtorHttpCall(call), file.absolutePath)
                    }
                }
            },
        )
        server.start(wait = false)
        engine = server
        port = runBlocking { server.engine.resolvedConnectors().first().port }
    }

    private fun get(pathAndQuery: String): HttpResponse<String> =
        HttpClient.newHttpClient().send(
            HttpRequest.newBuilder(URI("http://127.0.0.1:$port$pathAndQuery")).GET().build(),
            HttpResponse.BodyHandlers.ofString(),
        )

    private fun tempFile(suffix: String, content: ByteArray): File =
        File.createTempFile("sec_hdrs_", suffix).apply {
            deleteOnExit()
            writeBytes(content)
        }

    private fun tempFile(suffix: String, content: String): File = tempFile(suffix, content.toByteArray())
}
