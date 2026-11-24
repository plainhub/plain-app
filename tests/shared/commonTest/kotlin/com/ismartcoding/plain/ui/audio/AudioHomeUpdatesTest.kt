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
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class AudioHomeUpdatesTest {
    @Test
    fun selectedDeviceRefreshesOnAttachAndAudioChangesUntilThePageLeaves() = runTest {
        val now = Instant.fromEpochMilliseconds(0)
        val peer = Peer(
            id = "fixture-peer", name = "Fixture", ip = "device.test", port = 8443,
            status = PeerStatus.PAIRED, deviceType = DeviceType.COMPUTER,
            createdAt = now, updatedAt = now, publicKey = "fixture-key", token = "fixture-token",
        )
        var playlistReads = 0
        var overviewReads = 0
        var artist = "First artist"
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
                    """{"audioArtists":[{"name":"$artist","itemCount":1,"playCount":0,"samplePath":"/audio/track.wav"}],"mediaPlayHistory":[]}"""
                }
                "audios(" in operation.query -> """{"audios":[]}"""
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

        advanceTimeBy(60_000)
        peerUpdates.emit(SubscriptionUpdate.Data(GraphqlResponse(data = JsonHelper.jsonDecode(
            """{"mediaUpdated":{"types":["VIDEO"]}}""",
        ))))
        runCurrent()
        assertEquals(1, overviewReads)
        artist = "Updated artist"
        peerUpdates.emit(SubscriptionUpdate.Data(GraphqlResponse(data = JsonHelper.jsonDecode(
            """{"mediaUpdated":{"types":["AUDIO"]}}""",
        ))))
        runCurrent()
        assertEquals(2, overviewReads)
        assertEquals("Updated artist", viewModel.artists.value.single().name)

        peerUpdates.emit(SubscriptionUpdate.Attached)
        runCurrent()
        assertEquals(2, playlistReads)
        assertEquals(3, overviewReads)
        active.cancelAndJoin()
        peerUpdates.emit(SubscriptionUpdate.Data(GraphqlResponse(data = JsonHelper.jsonDecode("""{"contentChanged":true}"""))))
        runCurrent()
        assertEquals(3, overviewReads)

        val reentered = backgroundScope.launch { viewModel.observe(tags) }
        runCurrent()
        assertEquals(2, subscriptions.size)
        peerUpdates.emit(SubscriptionUpdate.Attached)
        runCurrent()
        assertEquals(3, playlistReads)
        assertEquals(4, overviewReads)
        reentered.cancelAndJoin()
    }
}
