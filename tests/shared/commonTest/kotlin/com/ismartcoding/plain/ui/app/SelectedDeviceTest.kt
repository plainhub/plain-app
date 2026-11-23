package com.ismartcoding.plain.ui.app

import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.features.chat.peer.ManagedDeviceClient
import com.ismartcoding.plain.features.nearby.PeerStatus
import com.ismartcoding.plain.models.Peer
import com.ismartcoding.plain.platform.system.DeviceType
import com.ismartcoding.plain.rust.graphql.ContentSource
import com.ismartcoding.plain.rust.graphql.PeerHttpClient
import com.ismartcoding.plain.ui.navigation.Routing
import com.ismartcoding.plain.ui.search.GlobalSearchAction
import com.ismartcoding.plain.ui.search.GlobalSearchHit
import kotlinx.coroutines.test.runTest
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

    @Test
    fun switch_device_saves_the_peer_and_startup_restores_it_even_when_offline() = runTest {
        val peer = remotePeer()
        val selected = DeviceIdentity(peer.id, peer.publicKey)
        var saved = ""

        MainViewModel.switchDevice(selected) { saved = it }
        assertEquals(peer.id, saved)
        assertEquals(selected, MainViewModel.selectedDevice.value)

        MainViewModel.selectDevice(null)
        MainViewModel.restoreDeviceSelection(saved, findPeer = { id -> peer.takeIf { it.id == id } },
            save = { fail("Restoring an authorized peer should not write the preference") })
        assertFalse(peer.online)
        assertEquals(selected, MainViewModel.selectedDevice.value)

        MainViewModel.switchDevice(null) { saved = it }
        assertEquals("", saved)
        assertNull(MainViewModel.selectedDevice.value)
    }

    @Test
    fun startup_returns_to_local_when_the_saved_peer_has_no_management_access() = runTest {
        val peer = remotePeer()
        for (unavailable in listOf(null, peer.copy(managed = false), peer.copy(publicKey = ""))) {
            var saved = peer.id
            MainViewModel.restoreDeviceSelection(saved, findPeer = { unavailable }, save = { saved = it })
            assertEquals("", saved)
            assertNull(MainViewModel.selectedDevice.value)
        }
    }

    @Test
    fun rejected_preference_write_preserves_the_current_device() = runTest {
        val peer = remotePeer()
        assertFailsWith<IllegalStateException> {
            MainViewModel.switchDevice(DeviceIdentity(peer.id, peer.publicKey)) { error("Write failed") }
        }
        assertNull(MainViewModel.selectedDevice.value)
    }

    private suspend fun remoteSource(): ContentSource.Remote {
        val peer = remotePeer()
        val response = """{"data":{"app":{"deviceName":"Remote","capabilities":[],"access":[],"appDir":"app://","downloadsDir":"/downloads","urlToken":"resource-token","tlsCertificateSha256":null}}}"""
        val http = PeerHttpClient(peer, peers = { listOf(peer) },
            executeGraphql = { _, _ -> JsonHelper.jsonDecode(response) },
        )
        return ContentSource.Remote(ManagedDeviceClient.load(peer, http))
    }

    private fun remotePeer(): Peer {
        val now = Instant.fromEpochMilliseconds(0)
        return Peer(
            id = "fixture-peer", name = "Remote", ip = "device.test", status = PeerStatus.PAIRED, port = 8443,
            deviceType = DeviceType.PHONE, createdAt = now, updatedAt = now,
            publicKey = "fixture-key", token = "fixture-token", managed = true,
        )
    }
}
