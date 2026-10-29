package com.ismartcoding.plain.ui.chat

import androidx.lifecycle.ViewModelStore
import com.ismartcoding.plain.features.nearby.PeerStatus
import com.ismartcoding.plain.models.Peer
import com.ismartcoding.plain.platform.system.DeviceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Instant

class PeerViewModelTest {
    @Test
    fun pairing_changes_never_render_the_same_peer_twice() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val time = Instant.fromEpochMilliseconds(0)
        val fixture = Peer(id = "fixture-peer", name = "Device", ip = "", port = 8443,
            status = PeerStatus.UNPAIRED, deviceType = DeviceType.PHONE,
            publicKey = "fixture-key", createdAt = time, updatedAt = time)
        var current = listOf(fixture)
        val vm = PeerViewModel(readPeers = { current }, readLatestChats = { emptyMap() })
        val store = ViewModelStore().apply { put("fixture", vm) }
        val rendered = mutableListOf<List<String>>()
        val collecting = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.peerList.collect { rendered += it.map { peer -> peer.id } }
        }
        try {
            vm.load()
            runCurrent()
            repeat(4) { index ->
                current = listOf(fixture.copy(status = if (index % 2 == 0) PeerStatus.PAIRED else PeerStatus.UNPAIRED))
                vm.load()
                runCurrent()
            }
            assertTrue(rendered.any { it == listOf(fixture.id) })
            assertTrue(rendered.dropWhile { it.isEmpty() }.all { it == listOf(fixture.id) }, "Pairing transitions duplicated or removed the peer: $rendered")
        } finally {
            collecting.cancel()
            store.clear()
            runCurrent()
            Dispatchers.resetMain()
        }
    }
}
