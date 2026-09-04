package com.ismartcoding.plain.httpserver

import com.ismartcoding.plain.lib.apk.cert.x509.X509SelfSignedGenerator
import com.ismartcoding.plain.lib.ktorserver.Netty
import com.ismartcoding.plain.lib.ktorserver.NettyApplicationEngine
import com.ismartcoding.plain.lib.ktorserver.core.engine.EngineSSLConnectorConfig
import com.ismartcoding.plain.lib.ktorserver.core.engine.EmbeddedServer
import com.ismartcoding.plain.lib.ktorserver.core.engine.applicationEnvironment
import com.ismartcoding.plain.lib.ktorserver.core.engine.connector
import com.ismartcoding.plain.lib.ktorserver.core.engine.embeddedServer
import com.ismartcoding.plain.lib.ktorserver.core.engine.sslConnector
import com.ismartcoding.plain.lib.ktorserver.core.request.receiveText
import com.ismartcoding.plain.lib.ktorserver.core.response.respond
import com.ismartcoding.plain.lib.ktorserver.core.response.respondText
import com.ismartcoding.plain.lib.ktorserver.core.routing.get
import com.ismartcoding.plain.lib.ktorserver.core.routing.post
import com.ismartcoding.plain.lib.ktorserver.core.routing.routing
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.time.Instant
import java.util.Date
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Concurrency regression lock (from the 2026-09 on-device hang): the engine
 * must never let file streaming block other connections. Serves real files
 * through the production LowMemoryFileContent path plus small JSON responses,
 * over plain HTTP and HTTPS, under concurrent load.
 */
class ConcurrencyStressTest {
    private val fileSize = 3L * 1024 * 1024
    private lateinit var file: File
    private var port: Int = 0
    private var sslPort: Int = 0
    private lateinit var engine: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>
    private val password = "stress-keystore"

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

        val generated = X509SelfSignedGenerator.newSelfSigned(
            "Plain",
            Date.from(Instant.parse("2026-01-01T00:00:00Z")),
            Date.from(Instant.parse("2040-01-01T00:00:00Z")),
        )
        val keyStore = KeyStore.getInstance("PKCS12").apply {
            load(null, null)
            setKeyEntry("plain", generated.privateKey, password.toCharArray(), arrayOf(generated.certificate))
        }

        engine = embeddedServer(
            Netty,
            applicationEnvironment { log = LoggerFactory.getLogger("concurrency-stress") },
            configure = {
                runningLimit = 32
                connector {
                    port = 0
                    host = "127.0.0.1"
                }
                sslConnector(
                    keyStore = keyStore,
                    keyAlias = "plain",
                    keyStorePassword = { password.toCharArray() },
                    privateKeyPassword = { password.toCharArray() },
                ) {
                    port = 0
                    host = "127.0.0.1"
                }
            },
            module = {
                routing {
                    get("/fs") {
                        val range = resolveSingleByteRange(call.request.headers["Range"], fileSize)
                        if (range == null) {
                            call.response.status(HttpStatusCode.RequestedRangeNotSatisfiable)
                            call.respondText("")
                            return@get
                        }
                        call.respond(
                            LowMemoryFileContent(
                                file = file,
                                contentType = ContentType.Application.OctetStream,
                                status = if (range.isPartial) HttpStatusCode.PartialContent else HttpStatusCode.OK,
                                contentLength = range.length,
                                range = range,
                                totalLength = fileSize,
                            )
                        )
                    }
                    post("/gql") {
                        call.receiveText()
                        call.respondText("""{"ok":true}""", ContentType.Application.Json)
                    }
                }
            },
        )
        engine.start(wait = false)
        runBlocking {
            port = engine.engine.resolvedConnectors().first { it !is EngineSSLConnectorConfig }.port
            sslPort = engine.engine.resolvedConnectors().filterIsInstance<EngineSSLConnectorConfig>().first().port
        }
        trustAll()
    }

    @AfterTest
    fun tearDown() {
        engine.stop(50, 500)
        file.delete()
    }

    @Test
    fun concurrentMixedRequests_http_allComplete() {
        runConcurrent("http", port, 48)
    }

    @Test
    fun concurrentMixedRequests_https_allComplete() {
        runConcurrent("https", sslPort, 24)
    }

    @Test
    fun browserStyleBursts_keepAlive_allComplete() {
        val pool = Executors.newFixedThreadPool(6)
        (0 until 6).map { conn ->
            pool.submit {
                repeat(8) { i ->
                    val path = if (i % 3 == 0) "/gql" else "/fs"
                    val connection = open("http", port, path, isPost = path == "/gql")
                    val body = connection.inputStream.use { it.readBytes() }
                    if (path == "/gql") assertEquals("""{"ok":true}""", body.decodeToString())
                    else assertEquals(fileSize.toInt(), body.size)
                }
            }
        }.forEach { it.get() }
        pool.shutdown()
    }

    private fun runConcurrent(scheme: String, port: Int, total: Int) {
        val completed = AtomicInteger(0)
        val failures = java.util.Collections.synchronizedList(mutableListOf<String>())
        val pool = Executors.newFixedThreadPool(12)
        (0 until total).map { i ->
            pool.submit {
                val path = if (i % 3 == 0) "/gql" else "/fs"
                try {
                    val start = System.currentTimeMillis()
                    val connection = open(scheme, port, path, isPost = path == "/gql")
                    val body = connection.inputStream.use { it.readBytes() }
                    val elapsed = System.currentTimeMillis() - start
                    if (path == "/gql") assertEquals("""{"ok":true}""", body.decodeToString())
                    else assertEquals(fileSize.toInt(), body.size)
                    completed.incrementAndGet()
                    if (elapsed > 5000) failures.add("$path took ${elapsed}ms")
                } catch (e: Exception) {
                    failures.add("req $i ($path) failed: $e")
                }
            }
        }.forEach { it.get() }
        pool.shutdown()
        assertEquals(total, completed.get(), "not all $scheme requests completed: $failures")
        assertTrue(failures.isEmpty(), "$scheme failures: $failures")
    }

    private fun open(scheme: String, port: Int, path: String, isPost: Boolean): HttpURLConnection {
        val connection = URL("$scheme://127.0.0.1:$port$path").openConnection() as HttpURLConnection
        connection.connectTimeout = 5000
        connection.readTimeout = 10_000
        if (isPost) {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.use { it.write("{}".toByteArray()) }
        }
        return connection
    }

    private fun trustAll() {
        val trustAll = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }
        val context = SSLContext.getInstance("TLS")
        context.init(null, arrayOf(trustAll), SecureRandom())
        HttpsURLConnection.setDefaultSSLSocketFactory(context.socketFactory)
        HttpsURLConnection.setDefaultHostnameVerifier { _, _ -> true }
    }
}
