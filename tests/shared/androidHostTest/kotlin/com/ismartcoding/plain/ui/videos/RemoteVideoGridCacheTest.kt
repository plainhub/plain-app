package com.ismartcoding.plain.ui.videos

import androidx.lifecycle.ViewModelStore
import coil3.decode.DataSource
import coil3.disk.DiskCache
import coil3.fetch.SourceFetchResult
import com.ismartcoding.plain.features.chat.peer.ManagedDeviceClient
import com.ismartcoding.plain.features.chat.peer.ResourceSize
import com.ismartcoding.plain.features.nearby.PeerStatus
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.models.Peer
import com.ismartcoding.plain.models.Video
import com.ismartcoding.plain.models.VideoSortBy
import com.ismartcoding.plain.platform.system.DeviceType
import com.ismartcoding.plain.rust.graphql.ContentSource
import com.ismartcoding.plain.rust.graphql.PeerHttpClient
import com.ismartcoding.plain.ui.tags.TagsViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okio.Path.Companion.toOkioPath
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class RemoteVideoGridCacheTest {
    @Test
    fun cached_thumbnail_is_read_before_authorizing_or_encrypting_a_remote_request() = runTest {
        withDisk { disk ->
            val fixture = Fixture()
            val client = fixture.client()
            val saved = assertNotNull(client.versionedThumbnail(fixture.video.path, ResourceSize.SM, fixture.video.updatedAt))
            val editor = assertNotNull(disk.openEditor(saved.cacheKey))
            disk.fileSystem.write(editor.metadata) { writeUtf8("fixture") }
            disk.fileSystem.write(editor.data) { writeUtf8("cached preview") }
            editor.commit()

            fixture.peer = fixture.peer.copy(ip = "new-address.test")
            val restarted = fixture.client()
            fixture.peerChecks = 0
            val restored = assertNotNull(restarted.versionedThumbnail(fixture.video.path, ResourceSize.SM, fixture.video.updatedAt))
            assertEquals(saved.cacheKey, restored.cacheKey)
            val hit = assertIs<SourceFetchResult>(restored.fetch(disk) { error("Disk cache hit requested the peer") })
            hit.source.use { assertEquals("cached preview", it.source().readUtf8()) }
            assertEquals(DataSource.DISK, hit.dataSource)
            assertEquals(0, fixture.peerChecks)
            assertEquals(0, fixture.encryptions)

            val revised = assertNotNull(restarted.versionedThumbnail(fixture.video.path, ResourceSize.SM, Instant.fromEpochMilliseconds(10)))
            assertNotEquals(saved.cacheKey, revised.cacheKey)
            assertNull(revised.fetch(disk) { request ->
                assertEquals("https://new-address.test:8443/fs?id=encrypted-reference&action=PREVIEW&size=SM&v=1970-01-01T00%3A00%3A00.010Z", request.url)
                null
            })
            assertEquals(1, fixture.peerChecks)
            assertEquals(1, fixture.encryptions)
            fixture.clientId = "other-client"
            assertEquals(saved.cacheKey, restarted.versionedThumbnail(fixture.video.path, ResourceSize.SM, fixture.video.updatedAt)?.cacheKey)
            fixture.clientId = "fixture-client"
            fixture.peer = fixture.peer.copy(id = "other-peer")
            val otherDevice = fixture.client()
            assertNotEquals(saved.cacheKey, otherDevice.versionedThumbnail(fixture.video.path, ResourceSize.SM, fixture.video.updatedAt)?.cacheKey)
        }
    }

    @Test
    fun remote_grid_renders_local_rows_while_the_first_server_page_is_pending() = runTest {
        withDisk { disk ->
            val queryStarted = CompletableDeferred<Unit>()
            val allowResponse = CompletableDeferred<Unit>()
            val fixture = Fixture {
                queryStarted.complete(Unit)
                allowResponse.await()
            }
            val client = fixture.client()
            val source = ContentSource.Remote(client)
            val cache = VideoGridCache(disk)
            val previous = fixture.video.copy(title = "Saved title")
            cache.write(client.videoCatalogCacheScope(), VideoSortBy.UPDATED_AT_DESC, listOf(previous))
            assertNull(cache.read(client.videoCatalogCacheScope(), VideoSortBy.UPDATED_AT_ASC))
            assertNull(cache.read("other-authority", VideoSortBy.UPDATED_AT_DESC))
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val store = ViewModelStore()
            try {
                val model = VideosViewModel(source, cache).also { store.put("videos", it) }
                val tags = TagsViewModel(source.tags).also { store.put("tags", it) }
                val load = model.enterPage(this, tags) {}
                queryStarted.await()
                assertEquals(listOf(previous), model.itemsFlow.value)
                allowResponse.complete(Unit)
                load.join()
                assertEquals(listOf(fixture.video), model.itemsFlow.value)
                assertEquals(listOf(fixture.video), cache.read(client.videoCatalogCacheScope(), VideoSortBy.UPDATED_AT_DESC))
                disk.clear()
                assertNull(cache.read(client.videoCatalogCacheScope(), VideoSortBy.UPDATED_AT_DESC))
            } finally {
                allowResponse.complete(Unit)
                store.clear()
                runCurrent()
                Dispatchers.resetMain()
            }
        }
    }

    private suspend fun withDisk(test: suspend (DiskCache) -> Unit) {
        val directory = createTempDirectory("video-grid-cache")
        val disk = DiskCache.Builder().directory(directory.toOkioPath()).build()
        try {
            test(disk)
        } finally {
            disk.shutdown()
            directory.toFile().deleteRecursively()
        }
    }

    private class Fixture(private val loading: suspend () -> Unit = {}) {
        val time = Instant.fromEpochMilliseconds(0)
        val video = Video("one", "Fresh title", "/videos/one.mp4", 1000, 1024, "videos", time, time, time)
        var peer = Peer(
            id = "fixture-peer", name = "Device", ip = "device.test", status = PeerStatus.PAIRED, port = 8443,
            deviceType = DeviceType.COMPUTER, createdAt = time, updatedAt = time, publicKey = "fixture-key", token = "fixture-token",
        )
        var peerChecks = 0
        var encryptions = 0
        var clientId = "fixture-client"

        suspend fun client() = ManagedDeviceClient.load(peer, PeerHttpClient(
            peer,
            peers = { peerChecks++; listOf(peer) },
            clientId = { clientId },
            encryptResourceReference = { _, _, _ -> encryptions++; "encrypted-reference" },
            executeGraphql = { _, operation ->
                val fields = when {
                    "videos(" in operation.query -> {
                        loading()
                        "\"videos\":${JsonHelper.jsonEncode(listOf(video))}"
                    }
                    "videoCount(" in operation.query -> "\"videoCount\":1"
                    "tagRelations(" in operation.query -> "\"tagRelations\":[]"
                    "tags(" in operation.query -> "\"tags\":[]"
                    "app {" in operation.query -> """"app":{"deviceName":"Device","capabilities":[],"access":["VIDEOS"],"appDir":"app://","downloadsDir":"/downloads","urlToken":"resource-token","tlsCertificateSha256":"fixture-certificate"}"""
                    else -> error("Unexpected operation: ${operation.query}")
                }
                JsonHelper.jsonDecode("""{"data":{$fields}}""")
            },
        ))
    }
}
