package com.ismartcoding.plain.rust.graphql

import com.ismartcoding.plain.models.ManagedResource
import okhttp3.Request
import java.io.IOException
import java.security.KeyFactory
import java.security.KeyStore
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLServerSocket
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PeerResourceHttpClientTest {
    private val certificate = javaClass.getResourceAsStream("/tls/resource-test-cert.pem")!!.use {
        CertificateFactory.getInstance("X.509").generateCertificate(it) as X509Certificate
    }
    private val fingerprint = MessageDigest.getInstance("SHA-256").digest(certificate.encoded)
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private fun server(): SSLServerSocket {
        val pem = javaClass.getResourceAsStream("/tls/resource-test-key.pem")!!.bufferedReader().use { it.readText() }
        val encoded = Base64.getMimeDecoder().decode(pem.replace("-----BEGIN PRIVATE KEY-----", "").replace("-----END PRIVATE KEY-----", ""))
        val key = KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(encoded))
        val store = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null)
            setKeyEntry("fixture", key, charArrayOf(), arrayOf(certificate))
        }
        val keys = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()).apply { init(store, charArrayOf()) }
        return SSLContext.getInstance("TLS").apply { init(keys.keyManagers, null, null) }
            .serverSocketFactory.createServerSocket(0) as SSLServerSocket
    }

    @Test
    fun authenticatedFingerprintAllowsSelfSignedResourceAndRange() {
        server().use { server ->
            val worker = Executors.newSingleThreadExecutor()
            try {
                val received = worker.submit<List<String>> {
                    server.accept().use { socket ->
                        val reader = socket.getInputStream().bufferedReader()
                        val headers = generateSequence { reader.readLine()?.takeIf { it.isNotEmpty() } }.toList()
                        socket.getOutputStream().write("HTTP/1.1 206 Partial Content\r\nContent-Length: 3\r\nConnection: close\r\n\r\nmp3".toByteArray())
                        headers
                    }
                }
                val resource = ManagedResource("https://127.0.0.1:${server.localPort}/fs?id=fixture", fingerprint)
                createPeerResourceHttpClient(resource).newCall(Request.Builder().url(resource.url).header("Range", "bytes=4-6").build()).execute().use {
                    assertEquals(206, it.code)
                    assertEquals("mp3", it.body.string())
                }
                assertTrue(received.get(5, TimeUnit.SECONDS).any { it.equals("Range: bytes=4-6", ignoreCase = true) })
            } finally { worker.shutdownNow() }
        }
    }

    @Test
    fun mismatchedCertificateRejectsConnectionBeforeSendingResourceId() {
        server().use { server ->
            val worker = Executors.newSingleThreadExecutor()
            try {
                val received = worker.submit<Boolean> {
                    server.accept().use { socket ->
                        try { socket.getInputStream().read() != -1 } catch (_: IOException) { false }
                    }
                }
                val resource = ManagedResource("https://127.0.0.1:${server.localPort}/fs?id=fixture", "0".repeat(64))
                assertFailsWith<SSLHandshakeException> {
                    createPeerResourceHttpClient(resource).newCall(Request.Builder().url(resource.url).build()).execute().close()
                }
                assertFalse(received.get(5, TimeUnit.SECONDS))
            } finally { worker.shutdownNow() }
        }
    }

    @Test
    fun peerResourcesRejectCleartextHttp() {
        assertFailsWith<IllegalArgumentException> {
            createPeerResourceHttpClient(ManagedResource("http://127.0.0.1/fs?id=fixture", fingerprint))
        }
    }
    @Test
    fun uploadRejectsAnotherCertificateBeforeSendingAnyFileBytes() = kotlinx.coroutines.runBlocking {
        server().use { server ->
            val worker = Executors.newSingleThreadExecutor()
            try {
                val received = worker.submit<Boolean> {
                    server.accept().use { socket ->
                        try {
                            val input = socket.getInputStream()
                            val receivedBytes = input.read() != -1
                            if (receivedBytes) socket.getOutputStream().write("HTTP/1.1 201 Created\r\nContent-Length: 3\r\nConnection: close\r\n\r\n0:1".toByteArray())
                            receivedBytes
                        } catch (_: IOException) { false }
                    }
                }
                kotlin.test.assertFailsWith<IOException> {
                    createGraphqlHttpTransport().post("https://127.0.0.1:${server.localPort}/upload_chunk", "fixture-client", byteArrayOf(42), "application/octet-stream", "0".repeat(64))
                }
                assertFalse(received.get(5, TimeUnit.SECONDS))
            } finally { worker.shutdownNow() }
        }
    }

    @Test
    fun uploadUsesTheAuthenticatedFingerprintAndSendsTheOriginalBytes() = kotlinx.coroutines.runBlocking {
        server().use { server ->
            val worker = Executors.newSingleThreadExecutor()
            try {
                val received = worker.submit<Pair<List<String>, Int>> {
                    server.accept().use { socket ->
                        val reader = socket.getInputStream().bufferedReader()
                        val headers = generateSequence { reader.readLine()?.takeIf { it.isNotEmpty() } }.toList()
                        val byte = reader.read()
                        socket.getOutputStream().write("HTTP/1.1 201 Created\r\nContent-Length: 3\r\nConnection: close\r\n\r\n0:1".toByteArray())
                        headers to byte
                    }
                }
                val response = createGraphqlHttpTransport().post("https://127.0.0.1:${server.localPort}/upload_chunk", "fixture-client", byteArrayOf(42), "application/octet-stream", fingerprint)
                assertEquals(201, response.status)
                assertEquals("0:1", response.body.decodeToString())
                val (headers, byte) = received.get(5, TimeUnit.SECONDS)
                assertTrue(headers.first().startsWith("POST /upload_chunk "))
                assertEquals(42, byte)
            } finally { worker.shutdownNow() }
        }
    }

}
