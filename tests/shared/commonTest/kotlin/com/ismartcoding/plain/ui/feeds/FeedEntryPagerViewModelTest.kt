package com.ismartcoding.plain.ui.feeds

import com.ismartcoding.plain.features.chat.peer.ManagedDeviceClient
import com.ismartcoding.plain.features.nearby.PeerStatus
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.models.Feed
import com.ismartcoding.plain.models.FeedEntry
import com.ismartcoding.plain.models.Peer
import com.ismartcoding.plain.platform.system.DeviceType
import com.ismartcoding.plain.rust.graphql.ContentSource
import com.ismartcoding.plain.rust.graphql.PeerHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class FeedEntryPagerViewModelTest {
    @Test
    fun neighborContentIsFetchedFromTheDeviceThatOwnsTheArticle() = runTest {
        val now = Instant.fromEpochMilliseconds(0)
        val peer = Peer(
            id = "fixture-peer", name = "Fixture", ip = "device.test", status = PeerStatus.PAIRED, port = 8443,
            deviceType = DeviceType.COMPUTER, createdAt = now, updatedAt = now,
            publicKey = "fixture-key", token = "fixture-token",
        )
        val entry = FeedEntry(id = "neighbor", feedId = "feed", description = "Summary")
        val full = entry.copy(content = "Full article", image = "image", updatedAt = Instant.fromEpochMilliseconds(1000))
        val requested = mutableListOf<String>()
        val http = PeerHttpClient(peer, peers = { listOf(peer) }, executeGraphql = { _, operation ->
            val fields = when {
                "app {" in operation.query ->
                    """"app":{"deviceName":"Fixture","capabilities":[],"access":[],"appDir":"app://","downloadsDir":"/downloads","urlToken":"fixture-resource","tlsCertificateSha256":"fixture-certificate"}"""
                "feedEntry(" in operation.query -> "\"feedEntry\":${JsonHelper.jsonEncode(entry)}"
                "feed(" in operation.query -> "\"feed\":${JsonHelper.jsonEncode(Feed(id = entry.feedId, fetchContent = true))}"
                "syncFeedEntryContent(" in operation.query -> {
                    requested.add(operation.variables.getValue("id").jsonPrimitive.content)
                    "\"syncFeedEntryContent\":${JsonHelper.jsonEncode(full)}"
                }
                else -> error("Unexpected operation: ${operation.query}")
            }
            JsonHelper.jsonDecode("""{"data":{$fields}}""")
        })
        val source = ContentSource.Remote(ManagedDeviceClient.load(peer, http))
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val pager = FeedEntryPagerViewModel(source)
            pager.setup(listOf("current", entry.id))
            pager.preloadAroundAsync("current")
            advanceUntilIdle()

            assertEquals(listOf(entry.id), requested)
            assertEquals(full.content, pager.cachedContent(entry.id))
            assertEquals(full, pager.entryAsync(entry.id))
        } finally {
            Dispatchers.resetMain()
        }
    }
}
