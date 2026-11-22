package com.ismartcoding.plain.features.chat.peer

import com.ismartcoding.plain.features.nearby.PeerStatus
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.models.Peer
import com.ismartcoding.plain.features.web.AccessFeature
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
import com.ismartcoding.plain.ui.components.ToastManager
import com.ismartcoding.plain.ui.tags.TagsViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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
    fun remote_media_timeout_surfaces_the_error_and_leaves_the_page_usable() = runTest {
        val peer = Peer(
            id = "fixture-peer", name = "Device", ip = "device.test", status = PeerStatus.PAIRED, port = 8443,
            deviceType = DeviceType.COMPUTER, createdAt = Instant.fromEpochMilliseconds(0), updatedAt = Instant.fromEpochMilliseconds(0),
            publicKey = "fixture-key", token = "fixture-token",
        )
        val response = """{"data":{"app":{"deviceName":"Device","capabilities":[],"access":["IMAGES","VIDEOS"],"appDir":"app://","downloadsDir":"/downloads","urlToken":"resource-token","tlsCertificateSha256":"fixture-certificate"}}}"""
        val requests = mutableListOf<GraphqlOperation>()
        var failWithTimeout = false
        val timeout = IllegalStateException("fixture response timeout")
        val http = PeerHttpClient(
            peer, peers = { listOf(peer) }, clientId = { "fixture-client" },
            encrypt = { _, _, payload -> requests.add(JsonHelper.jsonDecode(payload)); Base64.encode(byteArrayOf(1)) },
            decrypt = { _, _, _ -> Base64.encode(response.encodeToByteArray()) },
            transport = object : GraphqlHttpTransport {
                override suspend fun post(url: String, clientId: String, body: ByteArray, contentType: String, tlsCertificateSha256: String?): GraphqlHttpResponse {
                    if (failWithTimeout) throw timeout
                    return GraphqlHttpResponse(200, byteArrayOf(2))
                }
            },
        )
        val source = ContentSource.Remote(ManagedDeviceClient.load(peer, http))
        val failures = mutableListOf<Throwable>()
        val page = CoroutineScope(coroutineContext + SupervisorJob() + CoroutineExceptionHandler { _, error -> failures.add(error) })
        val previousToast = ToastManager.current.value
        failWithTimeout = true
        try {
            val tags = TagsViewModel(source.tags)
            var folderRequests = 0
            for ((field, vm) in listOf("videos" to VideosViewModel(source), "images" to ImagesViewModel(source))) {
                ToastManager.current.value = null
                vm.loadFromPage(page, tags) { folderRequests++ }.join()
                assertTrue(requests.last().query.contains("$field("))
                assertTrue(failures.isEmpty(), "A page load must not terminate the app")
                assertEquals(timeout.message, ToastManager.current.value?.message)
                assertFalse(vm.showLoading.value)
                assertTrue(vm.itemsFlow.value.isEmpty())
            }
            assertEquals(0, folderRequests)
        } finally {
            page.cancel()
            ToastManager.current.value = previousToast
        }
    }

    @Test
    fun entry_loads_access_once_and_media_starts_with_the_authoritative_grant() = runTest {
        val peer = Peer(
            id = "fixture-peer", name = "Device", ip = "device.test", status = PeerStatus.PAIRED, port = 8443,
            deviceType = DeviceType.PHONE, createdAt = Instant.fromEpochMilliseconds(0), updatedAt = Instant.fromEpochMilliseconds(0),
            publicKey = "fixture-key", token = "fixture-token",

        )
        var currentPeer = peer
        val requests = mutableListOf<GraphqlOperation>()
        var response = """{"data":{"app":{"deviceName":"Device","capabilities":[],"access":["IMAGES","AUDIO","VIDEOS","APPS"],"appDir":"app://","downloadsDir":"/downloads","urlToken":"resource-token","tlsCertificateSha256":"fixture-certificate"}}}"""
        val http = PeerHttpClient(peer, peers = { listOf(currentPeer) }, clientId = { "706c6169-b9ef-8b64-b37d-1162ef232835" },
            encryptResourceReference = { token, path ->
                assertEquals("resource-token", token)
                assertEquals("/target.mp3", path)
                "encrypted-resource"
            },
            encrypt = { _, _, payload -> requests.add(JsonHelper.jsonDecode(payload)); Base64.encode(byteArrayOf(1)) },
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
        assertEquals(listOf(AccessFeature.IMAGES, AccessFeature.AUDIO, AccessFeature.VIDEOS, AccessFeature.APPS), client.access.first())
        assertEquals(1, requests.size)
        assertTrue(requests.single().query.contains("access"))
        repeat(2) {
            val resource = client.resource("/target.mp3")
            assertEquals("https://device.test:8443/fs?id=encrypted-resource&action=PREVIEW", resource.url)
            assertEquals("fixture-certificate", resource.tlsCertificateSha256)
        }
        assertEquals(1, requests.size, "Resources reuse the authenticated entry metadata")
        currentPeer = peer.copy(token = "revoked-token")
        assertEquals("PEER_IDENTITY_CHANGED", assertFailsWith<IllegalStateException> { client.resource("/target.mp3") }.message)
        assertEquals(1, requests.size)
        currentPeer = peer
        response = """{"data":{"app":{"access":[]}}}"""
        client.refreshAccess()
        assertTrue(client.currentInfo.access.isEmpty())
        assertFalse(ImagesViewModel(source).hasPermission.value)
        assertFalse(AudioViewModel(source).hasPermission.value)
        assertFalse(VideosViewModel(source).hasPermission.value)
        assertFalse(AppsViewModel(source).hasPermission.value)
        assertTrue(client.access.first().isEmpty())
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
