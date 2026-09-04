package com.ismartcoding.plain.httpserver

import com.ismartcoding.plain.lib.apk.cert.x509.X509SelfSignedGenerator
import com.ismartcoding.plain.lib.ktorserver.PlainNettyServer
import com.ismartcoding.plain.lib.ktorserver.core.engine.EngineConnectorBuilder
import com.ismartcoding.plain.lib.ktorserver.core.engine.EngineSSLConnectorConfig
import com.ismartcoding.plain.lib.ktorserver.core.engine.connector
import com.ismartcoding.plain.lib.ktorserver.core.engine.sslConnector
import com.ismartcoding.plain.lib.ktorserver.core.request.path
import com.ismartcoding.plain.lib.ktorserver.core.request.receiveText
import com.ismartcoding.plain.lib.ktorserver.core.http.content.FileRegionContent
import com.ismartcoding.plain.lib.ktorserver.core.response.respond
import com.ismartcoding.plain.lib.ktorserver.core.response.respondText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Reproduces the field regression: many concurrent requests (file downloads +
 * small JSON) over separate keep-alive connections must all complete.
 */
class ConcurrencyStressTest {
    private val fileSize = 2L * 1024 * 1024
    private lateinit var file: File
    private var port: Int = 0
    private lateinit var engine: PlainNettyServer

    @BeforeTest
    fun setUp() {
        file = File.createTempFile("stress-test", ".bin")
        file.outputStream().use { out ->
            val buffer = ByteArray(64 * 1024)
            var written = 0L
            while (written < fileSize) {
                val len = minOf(buffer.size.toLong(), fileSize - written).toInt()
                for (i in 0 until len) buffer[i] = ((written + i) % 251).toByte()
                out.write(buffer, 0, len)
                written += len
            }
        }
        engine = PlainNettyServer(
            requestHandler = { call ->
                when (call.request.path()) {
                    "/fs" -> call.respond(
                        FileRegionContent(
                            file = file,
                            offset = 0,
                            length = fileSize,
                            contentType = ContentType.Application.OctetStream,
                            status = HttpStatusCode.OK,
                        )
                    )
                    "/gql" -> {
                        call.receiveText()
                        call.respondText("""{"ok":true}""", ContentType.Application.Json)
                    }
                }
            },
        ) {
            runningLimit = 32
            tcpKeepAlive = true
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
    }

    @Test
    fun concurrentOverHttps_allComplete() {
        val password = "stress-keystore"
        val generated = X509SelfSignedGenerator.newSelfSigned(
            "Plain",
            java.util.Date.from(java.time.Instant.parse("2026-01-01T00:00:00Z")),
            java.util.Date.from(java.time.Instant.parse("2040-01-01T00:00:00Z")),
        )
        val keyStore = java.security.KeyStore.getInstance("PKCS12").apply {
            load(null, null)
            setKeyEntry("plain", generated.privateKey, password.toCharArray(), arrayOf(generated.certificate))
        }
        val sslEngine = PlainNettyServer(
            requestHandler = { call ->
                when (call.request.path()) {
                    "/fs" -> call.respond(
                        FileRegionContent(
                            file = file,
                            offset = 0,
                            length = fileSize,
                            contentType = ContentType.Application.OctetStream,
                            status = HttpStatusCode.OK,
                        )
                    )
                    "/gql" -> {
                        call.receiveText()
                        call.respondText("""{"ok":true}""", ContentType.Application.Json)
                    }
                }
            },
        ) {
            runningLimit = 32
            tcpKeepAlive = true
            sslConnector(
                keyStore = keyStore,
                keyAlias = "plain",
                keyStorePassword = { password.toCharArray() },
                privateKeyPassword = { password.toCharArray() },
            ) {
                port = 0
                host = "127.0.0.1"
            }
        }
        sslEngine.start(wait = false)
        val sslPort = runBlocking { sslEngine.resolvedConnectors().filterIsInstance<EngineSSLConnectorConfig>().first().port }
        trustAll()

        val failures = java.util.Collections.synchronizedList(mutableListOf<String>())
        val completed = AtomicInteger(0)
        val total = 24
        val pool = Executors.newFixedThreadPool(12)
        (0 until total).map { i ->
            pool.submit {
                try {
                    val path = if (i % 3 == 0) "/gql" else "/fs"
                    val start = System.currentTimeMillis()
                    val connection = openSsl("https://127.0.0.1:$sslPort$path")
                    val body = connection.inputStream.use { it.readBytes() }
                    val elapsed = System.currentTimeMillis() - start
                    if (path == "/gql") assertEquals("""{"ok":true}""", body.decodeToString())
                    else assertEquals(fileSize.toInt(), body.size)
                    completed.incrementAndGet()
                    if (elapsed > 5000) failures.add("$path took ${elapsed}ms")
                } catch (e: Exception) {
                    failures.add("req $i failed: $e")
                }
            }
        }.forEach { it.get() }
        pool.shutdown()
        assertEquals(total, completed.get(), "not all https requests completed")
        assertTrue(failures.isEmpty(), "https failures: $failures")
    }

    private fun openSsl(url: String): HttpURLConnection {
        val connection = URL(url).openConnection() as javax.net.ssl.HttpsURLConnection
        connection.connectTimeout = 5000
        connection.readTimeout = 10_000
        return connection
    }

    private fun trustAll() {
        val trustAll = object : javax.net.ssl.X509TrustManager {
            override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
        }
        val context = javax.net.ssl.SSLContext.getInstance("TLS")
        context.init(null, arrayOf(trustAll), java.security.SecureRandom())
        javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(context.socketFactory)
        javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier { _, _ -> true }
    }

    @Test
    fun concurrentMixedRequests_allComplete() {
        val pool = Executors.newFixedThreadPool(16)
        val completed = AtomicInteger(0)
        val failures = java.util.Collections.synchronizedList(mutableListOf<String>())
        val total = 48
        val jobs = (0 until total).map { i ->
            pool.submit {
                try {
                    val path = if (i % 3 == 0) "/gql" else "/fs"
                    val start = System.currentTimeMillis()
                    val connection = URL("http://127.0.0.1:$port$path").openConnection() as HttpURLConnection
                    connection.connectTimeout = 5000
                    connection.readTimeout = 10_000
                    val body = connection.inputStream.use { it.readBytes() }
                    val elapsed = System.currentTimeMillis() - start
                    if (path == "/gql") {
                        assertEquals("""{"ok":true}""", body.decodeToString())
                    } else {
                        assertEquals(fileSize.toInt(), body.size)
                    }
                    completed.incrementAndGet()
                    if (elapsed > 5000) failures.add("$path took ${elapsed}ms")
                } catch (e: Exception) {
                    failures.add("req $i failed: $e")
                }
            }
        }
        jobs.forEach { it.get() }
        pool.shutdown()
        assertEquals(total, completed.get(), "not all requests completed")
        assertTrue(failures.isEmpty(), "failures: $failures")
    }

    @Test
    fun sequentialRequests_sameConnection_allComplete() {
        // 10 sequential keep-alive requests via URLConnection's pooled sockets
        repeat(10) { i ->
            val path = if (i % 2 == 0) "/fs" else "/gql"
            val connection = URL("http://127.0.0.1:$port$path").openConnection() as HttpURLConnection
            connection.readTimeout = 10_000
            val body = connection.inputStream.use { it.readBytes() }
            if (path == "/gql") assertEquals("""{"ok":true}""", body.decodeToString())
            else assertEquals(fileSize.toInt(), body.size)
        }
    }

    @Test
    fun burstFromFewConnections_pipelinedStyle_completes() {
        runBlocking {
            // 6 connections × 8 sequential requests each — mirrors a browser
            (0 until 6).map { conn ->
                async(Dispatchers.IO) {
                    repeat(8) { i ->
                        val path = if (i % 3 == 0) "/gql" else "/fs"
                        val connection = URL("http://127.0.0.1:$port$path").openConnection() as HttpURLConnection
                        connection.readTimeout = 10_000
                        val body = connection.inputStream.use { it.readBytes() }
                        if (path == "/gql") assertEquals("""{"ok":true}""", body.decodeToString())
                        else assertEquals(fileSize.toInt(), body.size)
                    }
                }
            }.awaitAll()
        }
    }
}
