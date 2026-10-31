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
import kotlinx.coroutines.test.*
import kotlin.io.encoding.Base64
import kotlin.test.*
import kotlin.time.Instant

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ManagedDeviceViewModelTest {
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
            assertEquals("Renamed remote", model.state.value.info?.deviceName)
            assertNull(model.state.value.error)
            assertFalse(model.state.value.loading)
            assertEquals(1, fixture.requests.count { "updateDeviceName(" in it.query })
            assertEquals(2, fixture.requests.count { "query { app {" in it.query })
            fixture.fail = true
            assertFalse(model.refresh())
            assertEquals("Renamed remote", model.state.value.info?.deviceName)
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
                    return GraphqlHttpResponse(if (fail) 500 else 200, byteArrayOf(2))
                }
            },
        )
    }
}
