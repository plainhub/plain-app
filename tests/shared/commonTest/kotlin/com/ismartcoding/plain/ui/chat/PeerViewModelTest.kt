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
import kotlin.test.assertEquals
import kotlin.time.Instant

class PeerViewModelTest {
    @Test
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun recipient_observers_share_target_updates_and_reconnect_after_the_picker_reopens() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val events = kotlinx.coroutines.flow.MutableSharedFlow<Unit>()
        var connections = 0
        var closed = 0
        var peerReads = 0
        var channelReads = 0
        val changes = kotlinx.coroutines.flow.flow {
            connections++
            try { events.collect { emit(it) } }
            finally { closed++ }
        }
        val peers = PeerViewModel(
            readPeers = { peerReads++; emptyList() }, readLatestChats = { emptyMap() }, sourceChanges = changes,
        )
        val channels = ChannelViewModel(readChannels = { channelReads++; emptyList() }, sourceChanges = peers.sourceUpdates)
        val store = ViewModelStore().apply { put("peers", peers); put("channels", channels) }
        try {
            repeat(2) { opening ->
                val peerObserver = backgroundScope.launch { peers.observe() }
                val channelObserver = backgroundScope.launch { channels.observe() }
                runCurrent()
                assertEquals(opening + 1, connections)
                val peerBefore = peerReads
                val channelBefore = channelReads
                events.emit(Unit)
                runCurrent()
                assertEquals(peerBefore + 1, peerReads)
                assertEquals(channelBefore + 1, channelReads)
                peerObserver.cancel()
                channelObserver.cancel()
                runCurrent()
                assertEquals(opening + 1, closed)
            }
        } finally {
            store.clear()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

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
