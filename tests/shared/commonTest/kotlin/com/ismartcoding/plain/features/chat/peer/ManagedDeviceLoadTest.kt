package com.ismartcoding.plain.features.chat.peer

import com.ismartcoding.plain.features.nearby.PeerStatus
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.models.Peer
import com.ismartcoding.plain.models.PeerAddress
import com.ismartcoding.plain.platform.permissions.Permission
import com.ismartcoding.plain.platform.system.DeviceType
import com.ismartcoding.plain.rust.graphql.ContentSource
import com.ismartcoding.plain.rust.graphql.GraphqlHttpResponse
import com.ismartcoding.plain.rust.graphql.GraphqlHttpTransport
import com.ismartcoding.plain.rust.graphql.GraphqlOperation
import com.ismartcoding.plain.rust.graphql.PeerHttpClient
import com.ismartcoding.plain.ui.images.ImagesViewModel
import com.ismartcoding.plain.ui.audio.AudioViewModel
import com.ismartcoding.plain.ui.videos.VideosViewModel
import com.ismartcoding.plain.ui.apps.AppsViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import com.ismartcoding.plain.rust.graphql.GraphqlResponse
import com.ismartcoding.plain.rust.graphql.SubscriptionUpdate
import kotlinx.coroutines.test.runTest
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith
import kotlin.time.Instant

class ManagedDeviceLoadTest {
    @Test
    fun entry_loads_permissions_once_and_media_starts_with_the_authoritative_grant() = runTest {
        val peer = Peer(
            id = "fixture-peer", name = "Device", ip = "", status = PeerStatus.PAIRED, port = 8443,
            deviceType = DeviceType.PHONE, createdAt = Instant.fromEpochMilliseconds(0), updatedAt = Instant.fromEpochMilliseconds(0),
            publicKey = "fixture-key", token = "fixture-token",
            address = PeerAddress("device.test", "Device", "https://device.test:8443", "", ""),
        )
        var currentPeer = peer
        val requests = mutableListOf<GraphqlOperation>()
        var response = """{"data":{"app":{"deviceName":"Device","capabilities":[],"permissions":["WRITE_EXTERNAL_STORAGE","QUERY_ALL_PACKAGES"],"appDir":"app://","downloadsDir":"/downloads","urlToken":"resource-token","tlsCertificateSha256":"fixture-certificate"}}}"""
        val http = PeerHttpClient(peer, peers = { listOf(currentPeer) }, clientId = { "fixture-client" },
            encryptResourceReference = { token, path ->
                assertEquals("resource-token", token)
                assertEquals("/target.mp3", path)
                "encrypted-resource"
            },
            encrypt = { _, payload -> requests.add(JsonHelper.jsonDecode(payload)); Base64.encode(byteArrayOf(1)) },
            decrypt = { _, _, _ -> Base64.encode(response.encodeToByteArray()) },
            transport = object : GraphqlHttpTransport {
                override suspend fun post(url: String, clientId: String, body: ByteArray, contentType: String, tlsCertificateSha256: String?) = GraphqlHttpResponse(200, byteArrayOf(2))
            },
        )
        val subscribed = mutableListOf<GraphqlOperation>()
        val event = GraphqlResponse(data = JsonHelper.jsonDecode("""{"peersChanged":true}"""))
        val client = ManagedDeviceClient.load(peer, http, updates = { operations ->
            subscribed.addAll(operations)
            flowOf(SubscriptionUpdate.Attached, SubscriptionUpdate.Data(event), SubscriptionUpdate.Failed(IllegalStateException("target subscription closed")))
        })
        val source = ContentSource.Remote(client)
        assertTrue(ImagesViewModel(source).hasPermission.value)
        assertTrue(AudioViewModel(source).hasPermission.value)
        assertTrue(VideosViewModel(source).hasPermission.value)
        assertTrue(AppsViewModel(source).hasPermission.value)
        assertEquals(listOf(Permission.WRITE_EXTERNAL_STORAGE, Permission.QUERY_ALL_PACKAGES), client.permissions.first())
        assertEquals(1, requests.size)
        assertTrue(requests.single().query.contains("permissions"))
        repeat(2) {
            val resource = client.resource("/target.mp3")
            assertEquals("https://device.test:8443/fs?id=encrypted-resource", resource.url)
            assertEquals("fixture-certificate", resource.tlsCertificateSha256)
        }
        assertEquals(1, requests.size, "Resources reuse the authenticated entry metadata")
        currentPeer = peer.copy(token = "revoked-token")
        assertEquals("peer_identity_changed", assertFailsWith<IllegalStateException> { client.resource("/target.mp3") }.message)
        assertEquals(1, requests.size)
        currentPeer = peer
        response = """{"data":{"app":{"permissions":[]}}}"""
        client.refreshPermissions()
        assertTrue(client.currentInfo.permissions.isEmpty())
        assertFalse(ImagesViewModel(source).hasPermission.value)
        assertFalse(AudioViewModel(source).hasPermission.value)
        assertFalse(VideosViewModel(source).hasPermission.value)
        assertFalse(AppsViewModel(source).hasPermission.value)
        assertTrue(client.permissions.first().isEmpty())
        assertEquals(2, requests.size)
        assertFalse(ImagesViewModel().hasPermission.value)
        val received = mutableListOf<GraphqlResponse>()
        val operation = GraphqlOperation("subscription { peersChanged }")
        assertEquals("target subscription closed", assertFailsWith<IllegalStateException> {
            client.subscribe(operation).collect { received.add(it) }
        }.message)
        assertEquals(listOf(event), received)
        assertEquals(listOf(operation), subscribed)
        assertEquals(2, requests.size)
    }
}
