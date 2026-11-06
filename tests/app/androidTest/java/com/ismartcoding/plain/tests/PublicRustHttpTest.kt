package com.ismartcoding.plain.tests

import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.rust.RustRuntime

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.network.*
import com.ismartcoding.plain.testing.CryptoFixtures.chaCha20Encrypt
import com.ismartcoding.plain.testing.CryptoFixtures.chaCha20Decrypt
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID
import com.ismartcoding.plain.rust.native.RustNativeApi
import com.ismartcoding.plain.features.sessions.RustSessionStore
import com.ismartcoding.plain.helpers.Base64Lenient
import java.net.URLEncoder
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.X509TrustManager

@RunWith(AndroidJUnit4::class)
class PublicRustHttpTest {
    @Test
    fun rustPublicListenerServesTlsUploadsRangesAndPortal() = runBlocking {
        val oldService = Prefs.backgroundServiceEnabled.value
        val oldDesktop = Prefs.remoteAccess.value
        val oldHttp = Prefs.httpPort.value
        val oldHttps = Prefs.httpsPort.value
        val fixtureName = "synthetic-http-${UUID.randomUUID()}"
        var clientId = ""
        var key = ByteArray(0)
        val root = File(appContext.cacheDir, fixtureName).apply { mkdirs() }
        val payload = ByteArray(512 * 1024 + 17) { (it % 251).toByte() }
        try {
            Prefs.backgroundServiceEnabled.value = true
            Prefs.remoteAccess.value = true
            withIO { RustRuntime.start() }
            assertTrue(RustNativeApi.checkHttp())
            val port = Prefs.httpPort.value
            val tlsPort = Prefs.httpsPort.value
            val trust = object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) = Unit
                override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
            }
            val tls = SSLContext.getInstance("TLS").apply { init(null, arrayOf(trust), null) }
            (tls.socketFactory.createSocket("127.0.0.1", tlsPort) as SSLSocket).use {
                it.startHandshake()
                val certificate = it.session.peerCertificates.first() as X509Certificate
                assertArrayEquals(RustNativeApi.tlsCertificateSignature(), certificate.signature)
            }
            createTestHttpClient(TestHttpClientSpec.Local()).use { client ->
                client.get("http://127.0.0.1:$port/health").use { assertEquals(200, it.status.value); assertEquals("OK", it.bodyAsText()) }
                client.get("https://127.0.0.1:$tlsPort/health").use { assertEquals(200, it.status.value); assertEquals("OK", it.bodyAsText()) }
                client.request("HEAD", "http://127.0.0.1:$port/health").use { assertEquals(200, it.status.value); assertEquals(0, it.bodyAsBytes().size) }
                client.post("http://127.0.0.1:$port/init", ByteArray(0)).use { assertEquals(400, it.status.value) }
                client.get("http://127.0.0.1:$port/").use { assertEquals(200, it.status.value); assertTrue(it.bodyAsText().contains("window.__SERVER_TIME__=")) }
                RustSessionStore.create(fixtureName)
                val session = RustSessionStore.list().single { it.name == fixtureName }
                clientId = session.clientId
                key = Base64Lenient.decode(session.token)
                val initBody = chaCha20Encrypt(key, "authenticated-init".encodeToByteArray())
                client.post("http://127.0.0.1:$port/init", initBody, "application/octet-stream", mapOf("c-id" to clientId)).use {
                    val responseText = it.bodyAsText()
                    assertEquals(responseText, 200, it.status.value)
                    val response = Json.parseToJsonElement(responseText).jsonObject
                    assertTrue(response.getValue("signaturePublicKey").jsonPrimitive.content.isNotEmpty())
                    assertEquals("", response.getValue("password").jsonPrimitive.content)
                }
                val graphqlRequest = """{"query":"{ noteCount(query: \"all\") }","variables":{}}"""
                val replayBody = "${System.currentTimeMillis()}|${UUID.randomUUID()}|$graphqlRequest"
                val encryptedGraphqlRequest = chaCha20Encrypt(key, replayBody)
                client.post("http://127.0.0.1:$port/graphql", encryptedGraphqlRequest, "application/octet-stream", mapOf("c-id" to clientId)).use {
                    assertEquals(200, it.status.value)
                    val decryptedResponse = chaCha20Decrypt(key, it.bodyAsBytes())
                        ?: throw AssertionError("GraphQL response decryption failed")
                    val result = Json.parseToJsonElement(decryptedResponse.decodeToString()).jsonObject
                    assertNotNull(result.toString(), result["data"]?.jsonObject?.get("noteCount"))
                }
                val info = buildJsonObject { put("dir", root.absolutePath); put("replace", false); put("size", payload.size) }.toString()
                val encrypted = chaCha20Encrypt(key, "${System.currentTimeMillis()}|${UUID.randomUUID()}|$info".encodeToByteArray())
                val prefix = "--fixture\r\nContent-Disposition: form-data; name=\"info\"\r\n\r\n".encodeToByteArray()
                val middle = "\r\n--fixture\r\nContent-Disposition: form-data; name=\"file\"; filename=\"sample.bin\"\r\nContent-Type: application/octet-stream\r\n\r\n".encodeToByteArray()
                val end = "\r\n--fixture--\r\n".encodeToByteArray()
                val body = prefix + encrypted + middle + payload + end
                client.post("http://127.0.0.1:$port/upload", body, "multipart/form-data; boundary=fixture", mapOf("c-id" to clientId)).use {
                    assertEquals(it.bodyAsText(), 201, it.status.value)
                }
                val file = File(root, "sample.bin")
                assertArrayEquals(payload, file.readBytes())
                val id = URLEncoder.encode(ResourceCrypto.encrypt(file.absolutePath), "UTF-8")
                client.get("http://127.0.0.1:$port/fs?id=$id", mapOf("Range" to "bytes=19-39")).use {
                    assertEquals(206, it.status.value)
                    assertArrayEquals(payload.copyOfRange(19, 40), it.bodyAsBytes())
                }
                Prefs.remoteAccess.value = false
                client.get("http://127.0.0.1:$port/health").use { assertEquals(200, it.status.value) }
                client.post("http://127.0.0.1:$port/graphql", "{}".encodeToByteArray(), "application/json").use { assertEquals(404, it.status.value) }
            }
        } finally {
            if (clientId.isNotEmpty()) RustSessionStore.delete(clientId)
            root.deleteRecursively()
            Prefs.backgroundServiceEnabled.value = oldService
            Prefs.remoteAccess.value = oldDesktop
            Prefs.httpPort.value = oldHttp
            Prefs.httpsPort.value = oldHttps
        }
    }
}
