package com.ismartcoding.plain.ui.app

import com.ismartcoding.plain.features.chat.peer.ManagedDeviceClient
import com.ismartcoding.plain.features.nearby.PeerStatus
import com.ismartcoding.plain.models.Peer
import com.ismartcoding.plain.models.PeerAddress
import com.ismartcoding.plain.platform.system.DeviceType
import com.ismartcoding.plain.rust.graphql.ContentSource
import com.ismartcoding.plain.rust.graphql.GraphqlHttpResponse
import com.ismartcoding.plain.rust.graphql.GraphqlHttpTransport
import com.ismartcoding.plain.rust.graphql.PeerHttpClient
import com.ismartcoding.plain.ui.navigation.Routing
import com.ismartcoding.plain.ui.search.GlobalSearchAction
import com.ismartcoding.plain.ui.search.GlobalSearchHit
import kotlinx.coroutines.test.runTest
import kotlin.io.encoding.Base64
import kotlin.test.*
import kotlin.time.Instant

class SelectedDeviceTest {
    @AfterTest
    fun reset() {
        MainViewModel.selectDevice(null)
        MainViewModel.pendingSearchHit.value = null
        MainViewModel.currentRootTab.intValue = 1
    }

    @Test
    fun selecting_a_different_device_resets_navigation_and_retires_its_pending_destination() = runTest {
        val source = remoteSource()
        val hit = GlobalSearchHit("first-note", "First note", action = GlobalSearchAction.Navigate(Routing.NoteDetail("first-note")), device = source)
        MainViewModel.openSearchHit(hit)
        assertEquals(DeviceIdentity(source.client.peerId, source.client.publicKey), MainViewModel.selectedDevice.value)
        MainViewModel.currentRootTab.intValue = 2
        val second = DeviceIdentity("second", "second-key")
        MainViewModel.selectDevice(second)
        assertEquals(second, MainViewModel.selectedDevice.value)
        assertEquals(1, MainViewModel.currentRootTab.intValue)
        assertNull(MainViewModel.pendingSearchHit.value)
        MainViewModel.selectDevice(null)
        assertNull(MainViewModel.selectedDevice.value)
    }

    @Test
    fun selecting_a_repaired_device_uses_its_current_identity() {
        MainViewModel.selectDevice(DeviceIdentity("selected", "old-key"))
        MainViewModel.currentRootTab.intValue = 2
        val current = DeviceIdentity("selected", "new-key")
        MainViewModel.selectDevice(current)
        assertEquals(current, MainViewModel.selectedDevice.value)
        assertEquals(1, MainViewModel.currentRootTab.intValue)
    }

    @Test
    fun opening_another_result_on_the_same_device_preserves_the_device_scope_and_original_route() = runTest {
        val source = remoteSource()
        val device = DeviceIdentity(source.client.peerId, source.client.publicKey)
        MainViewModel.selectDevice(device)
        MainViewModel.currentRootTab.intValue = 3
        val route = Routing.NoteDetail("selected-note")
        val hit = GlobalSearchHit("selected-note", "Selected note", action = GlobalSearchAction.Navigate(route), device = source)
        MainViewModel.openSearchHit(hit)
        assertSame(device, MainViewModel.selectedDevice.value)
        assertEquals(3, MainViewModel.currentRootTab.intValue)
        assertEquals(hit, MainViewModel.pendingSearchHit.value)
        assertEquals(route, (MainViewModel.pendingSearchHit.value!!.action as GlobalSearchAction.Navigate).route)
    }

    @Test
    fun opening_a_native_result_leaves_the_remote_scope() {
        MainViewModel.selectDevice(DeviceIdentity("selected", "selected-key"))
        val hit = GlobalSearchHit("local-note", "Local note", action = GlobalSearchAction.Navigate(Routing.NoteDetail("local-note")))
        MainViewModel.openSearchHit(hit)
        assertNull(MainViewModel.selectedDevice.value)
        assertEquals(1, MainViewModel.currentRootTab.intValue)
        assertEquals(hit, MainViewModel.pendingSearchHit.value)
    }

    private suspend fun remoteSource(): ContentSource.Remote {
        val now = Instant.fromEpochMilliseconds(0)
        val peer = Peer(
            id = "fixture-peer", name = "Remote", ip = "", status = PeerStatus.PAIRED, port = 8443,
            deviceType = DeviceType.PHONE, createdAt = now, updatedAt = now,
            publicKey = "fixture-key", token = "fixture-token",
            address = PeerAddress("device.test", "Remote", "https://device.test:8443", "", ""),
        )
        val response = """{"data":{"app":{"deviceName":"Remote","capabilities":[],"permissions":[],"appDir":"app://","downloadsDir":"/downloads","urlToken":"resource-token","tlsCertificateSha256":null}}}"""
        val http = PeerHttpClient(peer, peers = { listOf(peer) }, clientId = { "fixture-client" },
            encrypt = { _, _ -> Base64.encode(byteArrayOf(1)) },
            decrypt = { _, _, _ -> Base64.encode(response.encodeToByteArray()) },
            transport = object : GraphqlHttpTransport {
                override suspend fun post(url: String, clientId: String, body: ByteArray, contentType: String, tlsCertificateSha256: String?) = GraphqlHttpResponse(200, byteArrayOf(2))
            },
        )
        return ContentSource.Remote(ManagedDeviceClient.load(peer, http))
    }
}
