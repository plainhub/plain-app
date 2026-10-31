package com.ismartcoding.plain.ui.tools

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.ViewModelProvider
import com.ismartcoding.plain.features.nearby.PeerStatus
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.models.Peer
import com.ismartcoding.plain.models.PeerAddress
import com.ismartcoding.plain.platform.system.DeviceType
import com.ismartcoding.plain.rust.graphql.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.*
import kotlin.io.encoding.Base64
import kotlin.test.*
import kotlin.time.Instant

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ManagedDeviceViewModelTest {
    @Test
    fun only_first_entry_shows_loading_while_refresh_preserves_content_or_error() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            for (entryFails in listOf(false, true)) {
                val store = ViewModelStore()
                try {
                    val fixture = Fixture().apply {
                        fail = entryFails
                        responseGate = CompletableDeferred()
                    }
                    val model = ViewModelProvider.create(store, viewModelFactory {
                        initializer { ManagedDeviceViewModel(fixture.peer.id, fixture.peer.publicKey, { listOf(fixture.currentPeer) }, { fixture.http }) }
                    })[ManagedDeviceViewModel::class]
                    runCurrent()
                    assertTrue(model.state.value.loading)
                    assertNull(model.state.value.client)
                    fixture.responseGate!!.complete(Unit)
                    runCurrent()
                    assertFalse(model.state.value.loading)
                    assertEquals(if (entryFails) "HTTP 500" else null, model.state.value.error)
                    val previous = model.state.value

                    fixture.fail = false
                    fixture.responseGate = CompletableDeferred()
                    val refresh = async { model.refresh() }
                    runCurrent()
                    assertFalse(model.state.value.loading, "Pull refresh must not show the entry skeleton")
                    assertSame(previous.client, model.state.value.client)
                    assertEquals(previous.error, model.state.value.error)
                    fixture.responseGate!!.complete(Unit)
                    assertTrue(refresh.await())
                    assertFalse(model.state.value.loading)
                    assertNull(model.state.value.error)
                    assertNotNull(model.state.value.client)
                    if (!entryFails) assertSame(previous.client, model.state.value.client)
                } finally {
                    store.clear()
                }
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun rename_after_failed_entry_uses_remote_mutation_and_reloads_remote_identity() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        try {
            val fixture = Fixture()
            val model = ViewModelProvider.create(store, viewModelFactory {
                initializer { ManagedDeviceViewModel(fixture.peer.id, fixture.peer.publicKey, { listOf(fixture.currentPeer) }, { fixture.http }) }
            })[ManagedDeviceViewModel::class]
            runCurrent()
            assertEquals("HTTP 500", model.state.value.error)
            assertNull(model.state.value.client)
            fixture.fail = false
            model.rename("Renamed remote")
            assertEquals("Renamed remote", model.state.value.client?.currentInfo?.deviceName)
            assertNull(model.state.value.error)
            assertFalse(model.state.value.loading)
            assertEquals(1, fixture.requests.count { "updateDeviceName(" in it.query })
            assertEquals(2, fixture.requests.count { "query { app {" in it.query })
            fixture.fail = true
            assertFalse(model.refresh())
            assertEquals("Renamed remote", model.state.value.client?.currentInfo?.deviceName)
            assertEquals("HTTP 500", model.state.value.error)
            fixture.fail = false
            assertTrue(model.refresh())
            assertNull(model.state.value.error)
            fixture.currentPeer = fixture.peer.copy(publicKey = "changed-identity")
            val before = fixture.requests.size
            assertEquals("peer_identity_changed", assertFailsWith<IllegalStateException> { model.rename("Forbidden rename") }.message)
            assertEquals(before, fixture.requests.size)
        } finally {
            store.clear()
            Dispatchers.resetMain()
        }
    }

    private class Fixture {
        val peer = Peer(
            id = "fixture-peer", name = "Remote", ip = "", status = PeerStatus.PAIRED, port = 8443,
            deviceType = DeviceType.PHONE, createdAt = Instant.fromEpochMilliseconds(0), updatedAt = Instant.fromEpochMilliseconds(0),
            publicKey = "fixture-key", token = "fixture-token",
            address = PeerAddress("device.test", "Remote", "https://device.test:8443", "", ""),
        )
        var currentPeer = peer
        var fail = true
        var responseGate: CompletableDeferred<Unit>? = null
        var name = peer.name
        val requests = mutableListOf<GraphqlOperation>()
        private lateinit var operation: GraphqlOperation
        val http = PeerHttpClient(peer, peers = { listOf(currentPeer) }, clientId = { "fixture-client" },
            encrypt = { _, payload ->
                operation = JsonHelper.jsonDecode(payload)
                requests.add(operation)
                Base64.encode(byteArrayOf(1))
            },
            decrypt = { _, _, _ ->
                val response = if ("updateDeviceName(" in operation.query) {
                    name = operation.variables.getValue("name").let { (it as kotlinx.serialization.json.JsonPrimitive).content }
                    """{"data":{"updateDeviceName":true}}"""
                } else """{"data":{"app":{"deviceName":"$name","capabilities":[],"permissions":[],"appDir":"app://","downloadsDir":"/downloads","urlToken":"fixture-resource","tlsCertificateSha256":null}}}"""
                Base64.encode(response.encodeToByteArray())
            },
            transport = object : GraphqlHttpTransport {
                override suspend fun post(url: String, clientId: String, body: ByteArray, contentType: String, tlsCertificateSha256: String?): GraphqlHttpResponse {
                    assertEquals("https://device.test:8443/graphql", url)
                    responseGate?.await()
                    return GraphqlHttpResponse(if (fail) 500 else 200, byteArrayOf(2))
                }
            },
        )
    }
}
