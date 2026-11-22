package com.ismartcoding.plain.features.chat.peer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.unit.Density
import com.ismartcoding.plain.features.files.FileAction
import com.ismartcoding.plain.features.nearby.PeerStatus
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.models.Peer
import com.ismartcoding.plain.models.Video
import com.ismartcoding.plain.features.web.AccessFeature
import com.ismartcoding.plain.platform.system.DeviceType
import com.ismartcoding.plain.platform.system.imageCacheKey
import com.ismartcoding.plain.rust.graphql.ContentSource
import com.ismartcoding.plain.rust.graphql.GraphqlHttpException
import com.ismartcoding.plain.rust.graphql.GraphqlOperation
import com.ismartcoding.plain.rust.graphql.PeerHttpClient
import com.ismartcoding.plain.ui.images.ImagesViewModel
import com.ismartcoding.plain.ui.audio.AudioViewModel
import com.ismartcoding.plain.ui.videos.VideosViewModel
import com.ismartcoding.plain.ui.videos.remoteVideosCacheWindow
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.yield
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith
import kotlin.time.Instant

class ManagedDeviceLoadTest {
    @Test
    fun remote_file_ids_are_reused_for_the_same_reference_and_session() = runTest {
        val peer = Peer(
            id = "fixture-peer", name = "Device", ip = "device.test", status = PeerStatus.PAIRED, port = 8443,
            deviceType = DeviceType.COMPUTER, createdAt = Instant.fromEpochMilliseconds(0), updatedAt = Instant.fromEpochMilliseconds(0),
            publicKey = "fixture-key", token = "fixture-pairing-token",
        )
        var urlToken = "resource-token"
        var clientId = "fixture-client"
        var currentPeer = peer
        var encryptions = 0
        val http = PeerHttpClient(
            peer, peers = { listOf(currentPeer) }, clientId = { clientId },
            encryptResourceReference = { _, _, _ ->
                yield()
                "encrypted-${++encryptions}"
            },
            executeGraphql = { _, _ ->
                JsonHelper.jsonDecode("""{"data":{"app":{"deviceName":"Device","capabilities":[],"access":["IMAGES"],"appDir":"app://","downloadsDir":"/downloads","urlToken":"$urlToken","tlsCertificateSha256":"fixture-certificate"}}}""")
            },
        )
        val client = ManagedDeviceClient.load(peer, http)
        val path = "/fixture/photo.jpg"

        val thumbnails = List(3) { async { client.resource(path, ResourceSize.SM) } }.awaitAll()
        assertEquals(1, thumbnails.distinct().size)
        val thumbnail = thumbnails.first()
        assertEquals(thumbnail, client.resource(path, ResourceSize.SM))
        val versioned = client.resource(path, ResourceSize.SM, version = Instant.fromEpochMilliseconds(1))
        assertEquals(thumbnail.url + "&v=1970-01-01T00%3A00%3A00.001Z", versioned.url)
        assertNull(thumbnail.imageCacheKey())
        assertEquals(versioned.imageCacheKey(), client.resource(path, ResourceSize.SM, version = Instant.fromEpochMilliseconds(1)).imageCacheKey())
        assertNotEquals(versioned.imageCacheKey(), client.resource(path, ResourceSize.SM, version = Instant.fromEpochMilliseconds(2)).imageCacheKey())
        assertNotEquals(versioned.imageCacheKey(), versioned.copy(tlsCertificateSha256 = "other-pin").imageCacheKey())
        assertEquals("https://device.test:8443/fs?id=encrypted-1&action=PREVIEW&size=LG", client.resource(path, ResourceSize.LG).url)
        assertEquals("https://device.test:8443/fs?id=encrypted-1&action=DOWNLOAD", client.resource(path, action = FileAction.DOWNLOAD).url)
        assertEquals(1, encryptions)
        assertNotEquals(thumbnail, client.resource("/fixture/other.jpg", ResourceSize.SM))

        urlToken = "new-resource-token"
        client.info()
        val rotated = client.resource(path, ResourceSize.SM)
        assertNotEquals(thumbnail, rotated)
        assertEquals(rotated, client.resource(path, ResourceSize.SM))

        clientId = "new-client"
        assertNotEquals(rotated, client.resource(path, ResourceSize.SM))
        assertEquals(4, encryptions)

        currentPeer = peer.copy(token = "changed-pairing-token")
        assertEquals("PEER_IDENTITY_CHANGED", assertFailsWith<IllegalStateException> { client.resource(path, ResourceSize.SM) }.message)
    }

    @Test
    @OptIn(ExperimentalFoundationApi::class)
    fun remote_video_grid_bounds_offscreen_thumbnail_work() = with(remoteVideosCacheWindow) {
        with(Density(2f)) {
            assertEquals(300, calculateAheadWindow(1200))
            assertEquals(0, calculateBehindWindow(1200))
        }
    }

    @Test
    fun remote_videos_load_small_pages_and_retry_the_same_offset() = runTest {
        val instant = Instant.fromEpochMilliseconds(0)
        val peer = Peer(
            id = "fixture-peer", name = "Device", ip = "device.test", status = PeerStatus.PAIRED, port = 8443,
            deviceType = DeviceType.COMPUTER, createdAt = instant, updatedAt = instant,
            publicKey = "fixture-key", token = "fixture-token",
        )
        val clips = List(401) { index ->
            Video("clip-$index", "Clip $index", "/videos/$index.mp4", 1000, 1024, "videos", instant, instant, instant)
        }
        val pages = mutableListOf<Pair<Int, Int>>()
        var failNextPage = false
        val http = PeerHttpClient(peer, peers = { listOf(peer) }, executeGraphql = { _, operation ->
            val fields = when {
                "videos(" in operation.query -> {
                    val offset = operation.variables.getValue("offset").jsonPrimitive.int
                    val limit = operation.variables.getValue("limit").jsonPrimitive.int
                    pages.add(offset to limit)
                    if (failNextPage) throw GraphqlHttpException(500)
                    "\"videos\":${JsonHelper.jsonEncode(clips.drop(offset).take(limit))}"
                }
                "videoCount(" in operation.query -> "\"videoCount\":${clips.size}"
                "tagRelations(" in operation.query -> "\"tagRelations\":[]"
                "tags(" in operation.query -> "\"tags\":[]"
                "app {" in operation.query -> """"app":{"deviceName":"Device","capabilities":[],"access":["VIDEOS"],"appDir":"app://","downloadsDir":"/downloads","urlToken":"resource-token","tlsCertificateSha256":"fixture-certificate"}"""
                else -> error("Unexpected operation: ${operation.query}")
            }
            JsonHelper.jsonDecode("""{"data":{$fields}}""")
        })
        val source = ContentSource.Remote(ManagedDeviceClient.load(peer, http))
        val vm = VideosViewModel(source)
        val tags = TagsViewModel(source.tags)
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            vm.loadAsync(tags)
            assertEquals(listOf(0 to 128), pages)
            assertEquals(clips.take(128), vm.itemsFlow.value)
            assertFalse(vm.noMore.value)
            failNextPage = true
            assertFailsWith<GraphqlHttpException> { vm.moreAsync(tags) }
            assertEquals(clips.take(128), vm.itemsFlow.value)
            failNextPage = false
            vm.moreAsync(tags)
            assertEquals(clips.take(384), vm.itemsFlow.value)
            assertFalse(vm.noMore.value)
            vm.moreAsync(tags)
            assertEquals(listOf(0 to 128, 128 to 256, 128 to 256, 384 to 256), pages)
            assertEquals(clips, vm.itemsFlow.value)
            assertTrue(vm.noMore.value)
        } finally {
            Dispatchers.resetMain()
        }
    }

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
            executeGraphql = { _, operation ->
                requests.add(operation)
                if (failWithTimeout) throw timeout
                JsonHelper.jsonDecode(response)
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
            encryptResourceReference = { token, _, path ->
                assertEquals("resource-token", token)
                assertEquals("/target.mp3", path)
                "encrypted-resource"
            },
            executeGraphql = { _, operation ->
                requests.add(operation)
                JsonHelper.jsonDecode(response)
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
