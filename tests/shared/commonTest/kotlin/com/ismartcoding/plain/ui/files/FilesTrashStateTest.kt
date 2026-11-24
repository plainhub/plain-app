package com.ismartcoding.plain.ui.files

import com.ismartcoding.plain.features.chat.peer.ManagedDeviceClient
import com.ismartcoding.plain.features.nearby.PeerStatus
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.models.Peer
import com.ismartcoding.plain.platform.system.DeviceType
import com.ismartcoding.plain.rust.graphql.ContentSource
import com.ismartcoding.plain.rust.graphql.GraphqlResponse
import com.ismartcoding.plain.rust.graphql.PeerHttpClient
import com.ismartcoding.plain.rust.graphql.SubscriptionUpdate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class FilesTrashStateTest {
    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun refreshesOnAttachmentPushAndPullAndDiscardsResultsFromAnInactivePage() = runTest {
        val now = Instant.fromEpochMilliseconds(0)
        val peer = Peer(
            id = "fixture-peer", name = "Fixture", ip = "device.test", port = 8443,
            status = PeerStatus.PAIRED, deviceType = DeviceType.COMPUTER,
            createdAt = now, updatedAt = now, publicKey = "fixture-key", token = "fixture-token",
        )
        val updates = MutableSharedFlow<SubscriptionUpdate<GraphqlResponse>>()
        val contentChanged = SubscriptionUpdate.Data(response("""{"contentChanged":true}"""))
        var reads = 0
        var name = "initial.txt"
        var holdResponse: CompletableDeferred<Unit>? = null
        val http = PeerHttpClient(peer, peers = { listOf(peer) }, executeGraphql = { _, operation ->
            when {
                "app {" in operation.query -> response(
                    """{"app":{"deviceName":"Fixture","capabilities":[],"access":["FILES"],"appDir":"app://","downloadsDir":"/downloads","urlToken":"fixture-resource","tlsCertificateSha256":"fixture-certificate"}}""",
                )
                "trashedFiles(" in operation.query -> {
                    reads++
                    val snapshot = response(
                        """{"trashedFiles":[{"name":"$name","path":"/trash/$name","size":1,"createdAt":null,"updatedAt":"2026-01-01T00:00:00Z","isDir":false,"childCount":0}]}""",
                    )
                    holdResponse?.let { gate -> withContext(NonCancellable) { gate.await() } }
                    snapshot
                }
                else -> error("Unexpected operation: ${operation.query}")
            }
        })
        val source = ContentSource.Remote(ManagedDeviceClient.load(peer, http, updates = { updates }))
        val delivered = mutableListOf<String>()
        val state = FilesTrashState(source) { delivered.add(it.single().name) }
        val firstVisit = backgroundScope.launch { state.observe() }
        runCurrent()
        assertEquals(1, updates.subscriptionCount.value)
        assertEquals(0, reads)

        updates.emit(SubscriptionUpdate.Attached)
        runCurrent()
        advanceTimeBy(60_000)
        assertEquals(1, reads)
        assertEquals("initial.txt", state.files?.single()?.name)

        name = "push.txt"
        updates.emit(contentChanged)
        runCurrent()
        assertEquals(2, reads)
        name = "pull.txt"
        state.refresh()
        assertEquals(3, reads)
        assertEquals("pull.txt", state.files?.single()?.name)

        name = "late.txt"
        val lateResponse = CompletableDeferred<Unit>()
        holdResponse = lateResponse
        updates.emit(contentChanged)
        runCurrent()
        assertEquals(4, reads)
        firstVisit.cancel()
        lateResponse.complete(Unit)
        firstVisit.join()
        assertEquals(0, updates.subscriptionCount.value)
        updates.emit(contentChanged)
        assertEquals(4, reads)
        assertEquals("pull.txt", state.files?.single()?.name)

        holdResponse = null
        name = "return.txt"
        backgroundScope.launch { state.observe() }
        runCurrent()
        updates.emit(SubscriptionUpdate.Attached)
        runCurrent()
        name = "reconnect.txt"
        updates.emit(SubscriptionUpdate.Attached)
        runCurrent()
        assertEquals(6, reads)
        assertEquals(
            listOf("initial.txt", "push.txt", "pull.txt", "return.txt", "reconnect.txt"),
            delivered,
        )
        assertEquals("reconnect.txt", state.files?.single()?.name)
    }

    private fun response(json: String) = GraphqlResponse(data = JsonHelper.jsonDecode(json))
}
