package com.ismartcoding.plain.rust.graphql


import com.ismartcoding.plain.features.nearby.PeerStatus
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.models.Peer
import com.ismartcoding.plain.platform.system.DeviceType
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.io.encoding.Base64
import kotlin.test.*
import kotlin.time.Instant

class GraphqlHttpClientTest {
    private fun peer() = Peer(
        id = "706c6169-1c1b-84a0-8bc0-cc3821775aba", name = "Device", ip = "device.test", status = PeerStatus.PAIRED, port = 8443,
        deviceType = DeviceType.PHONE, createdAt = Instant.fromEpochMilliseconds(0), updatedAt = Instant.fromEpochMilliseconds(0),
        publicKey = "fixture-public-key", token = Base64.encode(ByteArray(32) { 7 }),

    )
    private val request = GraphqlRequest<String>("query { deviceName }", field = "deviceName")
    @Test
    fun remote_graphql_sends_the_selected_peer_and_not_the_old_address() = runTest {
        val saved = peer()
        var selected = saved
        val result = GraphqlResponse(data = JsonObject(mapOf("deviceName" to JsonPrimitive("Remote"))))
        val client = PeerHttpClient(
            saved, peers = { listOf(selected) },
            executeGraphql = { target, operation ->
                assertEquals(saved.id, target.id)
                assertEquals(saved.publicKey, target.publicKey)
                assertEquals(request.operation, operation)
                selected = selected.copy(ip = "new-address.test")
                result
            },
        )
        assertEquals("Remote", client.graphql.execute(request))
    }

    @Test
    fun remote_graphql_rejects_a_response_after_the_pairing_identity_changes() = runTest {
        val saved = peer()
        var selected = saved
        val client = PeerHttpClient(
            saved, peers = { listOf(selected) },
            executeGraphql = { _, _ ->
                selected = selected.copy(publicKey = "different-device")
                GraphqlResponse(data = JsonObject(mapOf("deviceName" to JsonPrimitive("Wrong peer"))))
            },
        )
        assertEquals("PEER_IDENTITY_CHANGED", assertFailsWith<IllegalStateException> {
            client.graphql.execute(request)
        }.message)
    }

    @Test
    fun rejects_removed_revoked_and_unpaired_devices_before_native_dispatch() = runTest {
        val saved = peer()
        for (rows in listOf(emptyList(), listOf(saved.copy(token = "")), listOf(saved.copy(status = PeerStatus.UNPAIRED)))) {
            val client = PeerHttpClient(saved, peers = { rows }, executeGraphql = { _, _ ->
                error("An unauthorized peer must not reach Core")
            })
            assertIs<com.ismartcoding.plain.models.BusinessException>(
                assertFailsWith<IllegalStateException> { client.graphql.execute(request) },
            )
        }
    }

    @Test
    fun uploads_binary_chunks_with_encrypted_metadata_and_exact_acknowledgement() = runTest {
        val peer = peer()
        val bytes = byteArrayOf(0, 13, 10, -1, 42)
        val encrypted = byteArrayOf(8, 0, -2)
        var posts = 0
        val client = PeerHttpClient(peer, peers = { listOf(peer) }, clientId = { "706c6169-a5ec-8cb0-b41d-c3030368d632" },
            encrypt = { token, endpoint, payload ->
                assertEquals(peer.token, token)
                assertEquals("/upload_chunk", endpoint)
                assertEquals(com.ismartcoding.plain.models.UploadChunkInfo("fixture-upload", 3, 5), JsonHelper.jsonDecode(payload))
                Base64.encode(encrypted)
            },
            transport = object : GraphqlHttpTransport {
                override suspend fun post(url: String, clientId: String, body: ByteArray, contentType: String, tlsCertificateSha256: String?): GraphqlHttpResponse {
                    posts++
                    assertEquals("https://device.test:8443/upload_chunk", url)
                    assertEquals("f".repeat(64), tlsCertificateSha256)
                    assertEquals("706c6169-a5ec-8cb0-b41d-c3030368d632", clientId)
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
                encrypt = { _, _, _ -> Base64.encode(byteArrayOf(1)) },
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
