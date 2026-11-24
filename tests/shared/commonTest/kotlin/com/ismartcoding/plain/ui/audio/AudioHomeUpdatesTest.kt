package com.ismartcoding.plain.ui.audio

import com.ismartcoding.plain.features.chat.peer.ManagedDeviceClient
import com.ismartcoding.plain.features.nearby.PeerStatus
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.models.Peer
import com.ismartcoding.plain.platform.system.DeviceType
import com.ismartcoding.plain.rust.graphql.ContentSource
import com.ismartcoding.plain.rust.graphql.GraphqlOperation
import com.ismartcoding.plain.rust.graphql.GraphqlResponse
import com.ismartcoding.plain.rust.graphql.PeerHttpClient
import com.ismartcoding.plain.rust.graphql.SubscriptionUpdate
import com.ismartcoding.plain.ui.tags.TagsViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class AudioHomeUpdatesTest {
    private fun audio(id: String, title: String = id) = """{"__typename":"Audio","id":"$id","title":"$title","artist":"Artist","path":"/music/$id.wav","durationMs":1234,"size":2000,"bucketId":"music","albumId":"album","albumFileId":"cover","createdAt":"2026-01-01T00:00:00Z","updatedAt":"2026-01-01T00:00:00Z","isFavorite":false}"""

    @Test
    fun recentOrderChangesOnlyOnPullToRefreshOrPageReentry() = runTest {
        val now = Instant.fromEpochMilliseconds(0)
        val peer = Peer(
            id = "fixture-peer", name = "Fixture", ip = "device.test", port = 8443,
            status = PeerStatus.PAIRED, deviceType = DeviceType.COMPUTER,
            createdAt = now, updatedAt = now, publicKey = "fixture-key", token = "fixture-token",
        )
        var playlistReads = 0
        var overviewReads = 0
        var artist = "First artist"
        var history = emptyList<String>()
        val catalog = mutableMapOf("first" to "first", "second" to "second", "third" to "third")
        var delayedOverview: CompletableDeferred<Unit>? = null
        val peerUpdates = MutableSharedFlow<SubscriptionUpdate<GraphqlResponse>>()
        val subscriptions = mutableListOf<List<GraphqlOperation>>()
        val http = PeerHttpClient(peer, peers = { listOf(peer) }, executeGraphql = { _, operation ->
            val data = when {
                "app {" in operation.query ->
                    """{"app":{"deviceName":"Fixture","capabilities":[],"access":["AUDIO"],"appDir":"app://","downloadsDir":"/downloads","urlToken":"fixture-resource","tlsCertificateSha256":"fixture-certificate"}}"""
                "audioPlaylists(" in operation.query || "audioPlaylists {" in operation.query -> {
                    playlistReads++
                    """{"audioPlaylists":[]}"""
                }
                "audioArtists {" in operation.query -> {
                    overviewReads++
                    val artists = """"audioArtists":[{"name":"$artist","itemCount":1,"playCount":0,"samplePath":"/audio/track.wav"}]"""
                    val media = when {
                        "mediaPlayHistory(" in operation.query -> {
                            val items = history.joinToString(",") { """{"media":${audio(it)}}""" }
                            ""","mediaPlayHistory":[$items]"""
                        }
                        "audios(" in operation.query -> {
                            val selection = operation.variables.getValue("query").jsonPrimitive.content
                            assertEquals("ids:=second,first", selection)
                            val ids = selection.removePrefix("ids:=").split(',')
                            assertEquals(ids.size, operation.variables.getValue("limit").jsonPrimitive.content.toInt())
                            val items = catalog.filterKeys { it in ids }.entries.joinToString(",") { audio(it.key, it.value) }
                            ""","audios":[$items]"""
                        }
                        else -> ""
                    }
                    val hold = delayedOverview
                    delayedOverview = null
                    hold?.await()
                    "{$artists$media}"
                }
                "tagRelations(" in operation.query -> """{"tagRelations":[]}"""
                else -> error("Unexpected operation: ${operation.query}")
            }
            GraphqlResponse(data = JsonHelper.jsonDecode(data))
        })
        val source = ContentSource.Remote(ManagedDeviceClient.load(peer, http, updates = {
            subscriptions.add(it)
            peerUpdates
        }))
        val viewModel = AudioHomeViewModel(source)
        val tags = TagsViewModel(source.tags)
        val active = backgroundScope.launch { viewModel.observe(tags) }
        runCurrent()

        assertEquals(
            listOf("subscription { contentChanged }", "subscription { mediaUpdated { types } }"),
            subscriptions.flatten().map { it.query },
        )
        assertEquals(0, overviewReads)
        peerUpdates.emit(SubscriptionUpdate.Attached)
        runCurrent()
        assertEquals(1, playlistReads)
        assertEquals(1, overviewReads)
        assertEquals("First artist", viewModel.artists.value.single().name)
        assertEquals(emptyList(), viewModel.recentItems.map { it.id })

        advanceTimeBy(60_000)
        peerUpdates.emit(SubscriptionUpdate.Data(GraphqlResponse(data = JsonHelper.jsonDecode(
            """{"mediaUpdated":{"types":["VIDEO"]}}""",
        ))))
        runCurrent()
        assertEquals(1, overviewReads)
        artist = "Updated artist"
        history = listOf("first")
        peerUpdates.emit(SubscriptionUpdate.Data(GraphqlResponse(data = JsonHelper.jsonDecode(
            """{"mediaUpdated":{"types":["AUDIO"]}}""",
        ))))
        runCurrent()
        assertEquals(2, overviewReads)
        assertEquals("Updated artist", viewModel.artists.value.single().name)
        assertEquals(emptyList(), viewModel.recentItems.map { it.id })

        history = listOf("second", "first")
        viewModel.refresh(tags)
        assertEquals(listOf("second", "first"), viewModel.recentItems.map { it.id })

        history = listOf("first", "second")
        catalog["first"] = "Updated title"
        peerUpdates.emit(SubscriptionUpdate.Data(GraphqlResponse(data = JsonHelper.jsonDecode("""{"contentChanged":true}"""))))
        runCurrent()
        assertEquals(listOf("second", "first"), viewModel.recentItems.map { it.id })
        assertEquals(listOf("second", "Updated title"), viewModel.recentItems.map { it.title })

        history = listOf("third", "first", "second")
        peerUpdates.emit(SubscriptionUpdate.Attached)
        runCurrent()
        assertEquals(3, playlistReads)
        assertEquals(5, overviewReads)
        assertEquals(listOf("second", "first"), viewModel.recentItems.map { it.id })

        viewModel.refresh(tags)
        assertEquals(listOf("third", "first", "second"), viewModel.recentItems.map { it.id })

        val late = CompletableDeferred<Unit>()
        delayedOverview = late
        history = listOf("second", "third", "first")
        artist = "Previous visit"
        val refreshing = backgroundScope.launch { viewModel.refresh(tags) }
        runCurrent()
        assertEquals(7, overviewReads)
        active.cancelAndJoin()
        peerUpdates.emit(SubscriptionUpdate.Data(GraphqlResponse(data = JsonHelper.jsonDecode("""{"contentChanged":true}"""))))
        runCurrent()
        assertEquals(7, overviewReads)

        history = listOf("first", "second", "third")
        artist = "Current visit"
        val reentered = backgroundScope.launch { viewModel.observe(tags) }
        runCurrent()
        assertEquals(2, subscriptions.size)
        peerUpdates.emit(SubscriptionUpdate.Attached)
        runCurrent()
        assertEquals(6, playlistReads)
        assertEquals(8, overviewReads)
        assertEquals(listOf("first", "second", "third"), viewModel.recentItems.map { it.id })
        late.complete(Unit)
        refreshing.join()
        assertEquals("Current visit", viewModel.artists.value.single().name)
        assertEquals(listOf("first", "second", "third"), viewModel.recentItems.map { it.id })

        peerUpdates.emit(SubscriptionUpdate.Failed(IllegalStateException("Stream closed")))
        runCurrent()
        assertEquals(listOf("first", "second", "third"), viewModel.recentItems.map { it.id })
        history = listOf("second", "first", "third")
        viewModel.refresh(tags)
        assertEquals(listOf("second", "first", "third"), viewModel.recentItems.map { it.id })
        reentered.cancelAndJoin()
    }
}
