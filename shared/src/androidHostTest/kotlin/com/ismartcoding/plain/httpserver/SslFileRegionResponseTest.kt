package com.ismartcoding.plain.httpserver

import com.ismartcoding.plain.lib.apk.cert.x509.X509SelfSignedGenerator
import com.ismartcoding.plain.lib.ktorserver.PlainNettyServer
import com.ismartcoding.plain.lib.ktorserver.core.engine.EngineConnectorBuilder
import com.ismartcoding.plain.lib.ktorserver.core.engine.EngineSSLConnectorConfig
import com.ismartcoding.plain.lib.ktorserver.core.engine.sslConnector
import com.ismartcoding.plain.lib.ktorserver.core.request.path
import com.ismartcoding.plain.lib.ktorserver.core.http.content.FileRegionContent
import com.ismartcoding.plain.lib.ktorserver.core.response.respond
import com.ismartcoding.plain.lib.ktorserver.core.response.respondText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URL
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.time.Instant
import java.util.Date
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Boots the real Netty engine behind its HTTPS connector and verifies that
 * FileRegionContent bodies arrive intact through the SslHandler, which cannot
 * write a FileRegion directly and must receive heap chunks instead.
 */
class SslFileRegionResponseTest {
    private val fileSize = 3L * 1024 * 1024
    private lateinit var file: File
    private var port: Int = 0
    private lateinit var engine: PlainNettyServer
    private val password = "test-keystore"

    @BeforeTest
    fun setUp() {
        file = File.createTempFile("ssl-fileregion-test", ".bin")
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

        engine = PlainNettyServer(
            requestHandler = { call ->
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
                    "/text" -> call.respondText("hello-ssl-region", ContentType.Text.Plain)
                }
            },
        ) {
            log = LoggerFactory.getLogger("ssl-fileregion-test")
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
        engine.start(wait = false)
        port = runBlocking {
            engine.resolvedConnectors().filterIsInstance<EngineSSLConnectorConfig>().first().port
        }
        trustAll()
    }

    @AfterTest
    fun tearDown() {
        engine.stop(50, 500)
        file.delete()
    }

    @Test
    fun fullFileResponse_overSsl_matchesBytes() {
        val connection = get("/file")
        assertEquals(200, connection.responseCode)
        assertEquals(fileSize.toString(), connection.getHeaderField("Content-Length"))
        val body = connection.inputStream.use { it.readBytes() }
        assertEquals(fileSize.toInt(), body.size)
        val expected = file.inputStream().use { it.readBytes() }
        assertTrue(expected.contentEquals(body), "ssl full file body mismatch")
    }

    @Test
    fun rangeResponse_overSsl_returnsExactSlice() {
        val connection = get("/range")
        assertEquals(206, connection.responseCode)
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
        assertTrue(expected.contentEquals(body), "ssl range body mismatch")
    }

    @Test
    fun smallResponse_overSsl_works() {
        val connection = get("/text")
        assertEquals(200, connection.responseCode)
        assertEquals("hello-ssl-region", connection.inputStream.use { it.readBytes().decodeToString() })
    }

    private fun trustAll() {
        val context = SSLContext.getInstance("TLS")
        context.init(
            null,
            arrayOf(
                object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                }
            ),
            SecureRandom(),
        )
        HttpsURLConnection.setDefaultSSLSocketFactory(context.socketFactory)
        HttpsURLConnection.setDefaultHostnameVerifier { _, _ -> true }
    }

    private fun get(path: String): HttpsURLConnection {
        val connection = URL("https://127.0.0.1:$port$path").openConnection() as HttpsURLConnection
        return connection
    }
}
