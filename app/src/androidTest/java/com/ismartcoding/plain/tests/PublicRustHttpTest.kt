package com.ismartcoding.plain.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.helpers.UrlHelper
import com.ismartcoding.plain.httpserver.HttpServerManager
import com.ismartcoding.plain.platform.*
import com.ismartcoding.plain.preferences.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.net.ServerSocket
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class PublicRustHttpTest {
    @Test
    fun rustPublicListenerServesTlsUploadsRangesPortalAndStops() = runBlocking {
        val oldService = UserPrefs.service.value
        val oldDesktop = UserPrefs.desktopAccess.value
        val oldHttp = UserPrefs.httpPort.value
        val oldHttps = UserPrefs.httpsPort.value
        val clientId = "synthetic-http-${UUID.randomUUID()}"
        val key = ByteArray(32) { 42 }
        val root = File(appContext.cacheDir, clientId).apply { mkdirs() }
        val payload = ByteArray(512 * 1024 + 17) { (it % 251).toByte() }
        try {
            UserPrefs.service.value = true
            UserPrefs.desktopAccess.value = true
            stopHttpEngineAsync()
            assertTrue(startHttpEngineAsync())
            val port = UserPrefs.httpPort.value
            val tlsPort = UserPrefs.httpsPort.value
            val trust = object : javax.net.ssl.X509TrustManager {
                override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) = Unit
                override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) = Unit
                override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = emptyArray()
            }
            val tls = javax.net.ssl.SSLContext.getInstance("TLS").apply { init(null, arrayOf(trust), null) }
            (tls.socketFactory.createSocket("127.0.0.1", tlsPort) as javax.net.ssl.SSLSocket).use {
                it.startHandshake()
                val certificate = it.session.peerCertificates.first() as java.security.cert.X509Certificate
                assertArrayEquals(getSSLSignature(SystemPrefs.keyStorePassword.value), certificate.signature)
            }
            createUnsafeHttpClient().use { client ->
                client.get("http://127.0.0.1:$port/health").use { assertEquals(200, it.status.value); assertEquals(getOwnPackageName(), it.bodyAsText()) }
                client.get("https://127.0.0.1:$tlsPort/health").use { assertEquals(200, it.status.value); assertEquals(getOwnPackageName(), it.bodyAsText()) }
                client.request("HEAD", "http://127.0.0.1:$port/health").use { assertEquals(200, it.status.value); assertEquals(0, it.bodyAsBytes().size) }
                client.post("http://127.0.0.1:$port/init", ByteArray(0)).use { assertEquals(400, it.status.value) }
                client.get("http://127.0.0.1:$port/").use { assertEquals(200, it.status.value); assertTrue(it.bodyAsText().contains("window.__SERVER_TIME__=")) }
                HttpServerManager.tokenCache.put(clientId, key)
                val info = buildJsonObject { put("dir", root.absolutePath); put("replace", false); put("size", payload.size) }.toString()
                val encrypted = chaCha20Encrypt(key, info.encodeToByteArray())
                val prefix = "--fixture\r\nContent-Disposition: form-data; name=\"info\"\r\n\r\n".encodeToByteArray()
                val middle = "\r\n--fixture\r\nContent-Disposition: form-data; name=\"file\"; filename=\"sample.bin\"\r\nContent-Type: application/octet-stream\r\n\r\n".encodeToByteArray()
                val end = "\r\n--fixture--\r\n".encodeToByteArray()
                val body = prefix + encrypted + middle + payload + end
                client.post("http://127.0.0.1:$port/upload", body, "multipart/form-data; boundary=fixture", mapOf("c-id" to clientId)).use {
                    assertEquals(it.bodyAsText(), 201, it.status.value)
                }
                val file = File(root, "sample.bin")
                assertArrayEquals(payload, file.readBytes())
                val id = java.net.URLEncoder.encode(UrlHelper.encrypt(file.absolutePath), "UTF-8")
                client.get("http://127.0.0.1:$port/fs?id=$id", mapOf("Range" to "bytes=19-39")).use {
                    assertEquals(206, it.status.value)
                    assertArrayEquals(payload.copyOfRange(19, 40), it.bodyAsBytes())
                }
                UserPrefs.desktopAccess.value = false
                client.get("http://127.0.0.1:$port/health").use { assertEquals(200, it.status.value) }
                client.post("http://127.0.0.1:$port/graphql", "{}".encodeToByteArray(), "application/json").use { assertEquals(404, it.status.value) }
            }
            stopHttpEngineAsync()
            for (released in listOf(port, tlsPort)) {
                ServerSocket().use { it.reuseAddress = true; it.bind(java.net.InetSocketAddress(released)); assertTrue(it.isBound) }
            }
        } finally {
            stopHttpEngineAsync()
            HttpServerManager.tokenCache.invalidate(clientId)
            root.deleteRecursively()
            UserPrefs.service.value = oldService
            UserPrefs.desktopAccess.value = oldDesktop
            UserPrefs.httpPort.value = oldHttp
            UserPrefs.httpsPort.value = oldHttps
        }
    }
}
