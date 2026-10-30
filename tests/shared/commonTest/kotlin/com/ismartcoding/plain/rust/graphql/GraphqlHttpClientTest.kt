package com.ismartcoding.plain.rust.graphql

import com.ismartcoding.plain.features.nearby.PeerStatus
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.models.Peer
import com.ismartcoding.plain.models.PeerAddress
import com.ismartcoding.plain.platform.system.DeviceType
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.io.encoding.Base64
import kotlin.test.*
import kotlin.time.Instant

class GraphqlHttpClientTest {
    private fun peer() = Peer(
        id = "fixture-peer", name = "Device", ip = "", status = PeerStatus.PAIRED, port = 8443,
        deviceType = DeviceType.PHONE, createdAt = Instant.fromEpochMilliseconds(0), updatedAt = Instant.fromEpochMilliseconds(0),
        publicKey = "fixture-public-key", token = Base64.encode(ByteArray(32) { 7 }),
        address = PeerAddress("device.test", "Device", "https://device.test:8443", "", ""),
    )
    private val request = GraphqlRequest<String>("query { deviceName }", field = "deviceName")
    private fun encodedResult() = Base64.encode(JsonHelper.jsonEncode(GraphqlResponse(
        data = JsonObject(mapOf("deviceName" to JsonPrimitive("Remote"))),
    )).encodeToByteArray())

    @Test
    fun binds_token_identity_and_request_response_without_native_data_fallback() = runTest {
        val peer = peer()
        var posts = 0
        val ciphertext = Base64.encode("request ciphertext".encodeToByteArray())
        val client = PeerHttpClient(peer,
            peers = { listOf(peer.copy()) }, clientId = { "local-client" },
            encrypt = { token, payload ->
                assertEquals(peer.token, token)
                assertEquals(request.operation, JsonHelper.jsonDecode<GraphqlOperation>(payload))
                ciphertext
            },
            decrypt = { token, body, response ->
                assertEquals(peer.token, token)
                assertEquals(ciphertext, body)
                assertEquals(Base64.encode("response ciphertext".encodeToByteArray()), response)
                encodedResult()
            },
            transport = object : GraphqlHttpTransport {
                override suspend fun post(url: String, clientId: String, body: ByteArray, contentType: String, tlsCertificateSha256: String?): GraphqlHttpResponse {
                    posts++
                    assertEquals("https://device.test:8443/graphql", url)
                    assertEquals("local-client", clientId)
                    assertContentEquals(Base64.decode(ciphertext), body)
                    return GraphqlHttpResponse(200, "response ciphertext".encodeToByteArray())
                }
            },
        ).graphql
        assertEquals("Remote", client.execute(request))
        assertEquals(1, posts)
    }

    @Test
    fun rejects_chat_only_removed_and_changed_credentials_before_network_effects() = runTest {
        val peer = peer()
        val cases = listOf(emptyList(), listOf(peer.copy(token = "")), listOf(peer.copy(status = PeerStatus.UNPAIRED)), listOf(peer.copy(publicKey = "changed")), listOf(peer.copy(token = "changed")))
        for (rows in cases) {
            val client = PeerHttpClient(peer, peers = { rows },
                encrypt = { _, _ -> error("Encryption must not start") },
                transport = object : GraphqlHttpTransport {
                    override suspend fun post(url: String, clientId: String, body: ByteArray, contentType: String, tlsCertificateSha256: String?): GraphqlHttpResponse = error("No network effects")
                },
            ).graphql
            assertFailsWith<IllegalStateException> { client.execute(request) }
        }
    }

    @Test
    fun chat_rekeying_does_not_replace_the_independent_management_identity() = runTest {
        val expected = peer().copy(key = "previous-chat-key")
        var current = expected.copy()
        var posts = 0
        val client = PeerHttpClient(expected, peers = { listOf(current) }, clientId = { "local-client" },
            encrypt = { _, _ -> Base64.encode(byteArrayOf(1)) }, decrypt = { _, _, _ -> encodedResult() },
            transport = object : GraphqlHttpTransport {
                override suspend fun post(url: String, clientId: String, body: ByteArray, contentType: String, tlsCertificateSha256: String?): GraphqlHttpResponse {
                    posts++
                    current = current.copy(key = "new-chat-key")
                    return GraphqlHttpResponse(200, byteArrayOf(2))
                }
            },
        ).graphql
        assertEquals("Remote", client.execute(request))
        assertEquals(1, posts)
    }

    @Test
    fun discards_a_response_when_the_selected_device_changes_in_flight() = runTest {
        val peer = peer()
        var current = peer.copy()
        val client = PeerHttpClient(peer, peers = { listOf(current) },
            encrypt = { _, _ -> Base64.encode(byteArrayOf(1)) }, decrypt = { _, _, _ -> encodedResult() },
            transport = object : GraphqlHttpTransport {
                override suspend fun post(url: String, clientId: String, body: ByteArray, contentType: String, tlsCertificateSha256: String?): GraphqlHttpResponse {
                    current = current.copy(publicKey = "different-device")
                    return GraphqlHttpResponse(200, byteArrayOf(2))
                }
            },
        ).graphql
        assertEquals("peer_identity_changed", assertFailsWith<IllegalStateException> { client.execute(request) }.message)
    }

    @Test
    fun does_not_decrypt_or_retry_rejected_sessions_and_redirects() = runTest {
        val peer = peer()
        for (status in listOf(301, 401, 403, 404, 500)) {
            var posts = 0
            val client = PeerHttpClient(peer, peers = { listOf(peer) },
                encrypt = { _, _ -> Base64.encode(byteArrayOf(1)) },
                decrypt = { _, _, _ -> error("Rejected response must not be decoded") },
                transport = object : GraphqlHttpTransport {
                    override suspend fun post(url: String, clientId: String, body: ByteArray, contentType: String, tlsCertificateSha256: String?): GraphqlHttpResponse {
                        posts++
                        return GraphqlHttpResponse(status, byteArrayOf())
                    }
                },
            ).graphql
            val error = assertFailsWith<IllegalStateException> { client.execute(request) }
            if (status != 401 && status != 403) {
                assertEquals(status, assertIs<GraphqlHttpException>(error).status)
                assertEquals("HTTP $status", error.message)
            }
            assertEquals(1, posts)
        }
    }
    @Test
    fun reports_a_request_timeout_as_a_recoverable_error() = runTest {
        val peer = peer()
        val client = PeerHttpClient(peer, peers = { listOf(peer) },
            encrypt = { _, _ -> Base64.encode(byteArrayOf(1)) },
            transport = object : GraphqlHttpTransport {
                override suspend fun post(url: String, clientId: String, body: ByteArray, contentType: String, tlsCertificateSha256: String?): GraphqlHttpResponse {
                    delay(60_000)
                    error("The timed out request must be cancelled")
                }
            },
        ).graphql
        assertEquals("connection_timeout", assertFailsWith<IllegalStateException> { client.execute(request) }.message)
    }

    @Test
    fun uploads_binary_chunks_with_encrypted_metadata_and_exact_acknowledgement() = runTest {
        val peer = peer()
        val bytes = byteArrayOf(0, 13, 10, -1, 42)
        val encrypted = byteArrayOf(8, 0, -2)
        var posts = 0
        val client = PeerHttpClient(peer, peers = { listOf(peer) }, clientId = { "local-client" },
            encrypt = { token, payload ->
                assertEquals(peer.token, token)
                assertEquals(com.ismartcoding.plain.models.UploadChunkInfo("fixture-upload", 3, 5), JsonHelper.jsonDecode(payload))
                Base64.encode(encrypted)
            },
            transport = object : GraphqlHttpTransport {
                override suspend fun post(url: String, clientId: String, body: ByteArray, contentType: String, tlsCertificateSha256: String?): GraphqlHttpResponse {
                    posts++
                    assertEquals("https://device.test:8443/upload_chunk", url)
                    assertEquals("f".repeat(64), tlsCertificateSha256)
                    assertEquals("local-client", clientId)
                    val boundary = contentType.substringAfter("boundary=")
                    val expected = "--$boundary\r\nContent-Disposition: form-data; name=\"info\"; filename=\"info\"\r\nContent-Type: application/octet-stream\r\n\r\n".encodeToByteArray() + encrypted +
                        "\r\n--$boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"chunk\"\r\nContent-Type: application/octet-stream\r\n\r\n".encodeToByteArray() + bytes + "\r\n--$boundary--\r\n".encodeToByteArray()
                    assertContentEquals(expected, body)
                    return GraphqlHttpResponse(201, "3:5".encodeToByteArray())
                }
            },
        )
        assertEquals("", client.uploadChunk("fixture-upload", 3, bytes, "f".repeat(64)))
        assertEquals(1, posts)
    }

    @Test
    fun rejects_wrong_upload_acknowledgements_without_retry() = runTest {
        val peer = peer()
        for ((status, acknowledgement) in listOf(200 to "0:1", 201 to "1:1", 201 to "0:2", 403 to "0:1")) {
            var posts = 0
            val client = PeerHttpClient(peer, peers = { listOf(peer) },
                encrypt = { _, _ -> Base64.encode(byteArrayOf(1)) },
                transport = object : GraphqlHttpTransport {
                    override suspend fun post(url: String, clientId: String, body: ByteArray, contentType: String, tlsCertificateSha256: String?): GraphqlHttpResponse {
                        posts++
                        return GraphqlHttpResponse(status, acknowledgement.encodeToByteArray())
                    }
                },
            )
            assertFailsWith<IllegalStateException> { client.uploadChunk("fixture-upload", 0, byteArrayOf(1), "f".repeat(64)) }
            assertEquals(1, posts)
        }
    }

}
