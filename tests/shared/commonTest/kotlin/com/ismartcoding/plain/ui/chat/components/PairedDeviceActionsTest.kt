package com.ismartcoding.plain.ui.chat.components

import com.ismartcoding.plain.features.nearby.PeerStatus
import com.ismartcoding.plain.models.Peer
import com.ismartcoding.plain.models.PairingProgressNotice
import com.ismartcoding.plain.platform.system.DeviceType
import com.ismartcoding.plain.ui.nearby.NearbyItemStatus
import com.ismartcoding.plain.ui.nearby.NearbyViewModel
import com.ismartcoding.plain.ui.nearby.PairingProjection
import com.ismartcoding.plain.ui.nearby.PairingStep
import com.ismartcoding.plain.ui.nearby.PairingView
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class PairedDeviceActionsTest {
    private val peer = Peer(id = "actions-test", name = "Test device", ip = "device.test", status = PeerStatus.PAIRED,
        port = 8443, deviceType = DeviceType.PHONE, createdAt = Instant.fromEpochMilliseconds(0), updatedAt = Instant.fromEpochMilliseconds(0))

    @AfterTest
    fun clear() {
        NearbyViewModel.pairingViews.clear()
        NearbyViewModel.itemStatus.clear()
    }

    @Test
    fun allPairingStepsHaveExclusiveControlsRegardlessOfCurrentDeviceOrConnectivity() {
        for (managed in listOf(false, true)) for (selected in listOf(false, true)) for (online in listOf(false, true)) {
            val device = peer.copy(token = if (managed) "test-token" else "", online = online)
            val resting = if (managed && !selected) PairedDeviceActions.SWITCH_AND_MORE else PairedDeviceActions.MORE
            assertEquals(resting, pairedDeviceActions(device, selected))
            for (step in PairingStep.entries) {
                NearbyViewModel.pairingViews[peer.id] = PairingView(peer.id, step)
                val pending = step in listOf(PairingStep.PAIRING, PairingStep.PASSWORD_REQUIRED, PairingStep.TRUST_REQUIRED, PairingStep.WAITING_FOR_PEER)
                assertEquals(if (pending) PairedDeviceActions.CANCEL else resting, pairedDeviceActions(device, selected), "$managed/$selected/$online/$step")
            }
            NearbyViewModel.pairingViews.clear()
            for (status in NearbyItemStatus.entries) {
                NearbyViewModel.itemStatus[peer.id] = status
                val pending = status == NearbyItemStatus.STARTING || status == NearbyItemStatus.PAIRING
                assertEquals(if (pending) PairedDeviceActions.CANCEL else resting, pairedDeviceActions(device, selected), "$managed/$selected/$online/$status")
            }
            NearbyViewModel.itemStatus.clear()
        }
    }

    @Test
    fun normalPairingNotificationsReplaceMoreWithCancelAndRestoreActionsOnCompletionOrCancellation() {
        val managed = peer.copy(token = "test-token")
        PairingProjection.starting(peer.id)
        assertEquals(PairedDeviceActions.CANCEL, pairedDeviceActions(peer, false))
        PairingProjection.started(PairingProgressNotice(peer.id, peer.name, verificationCode = "01234567"))
        assertEquals(PairedDeviceActions.CANCEL, pairedDeviceActions(managed, false))
        PairingProjection.completed(PairingProgressNotice(peer.id, peer.name))
        assertEquals(PairedDeviceActions.SWITCH_AND_MORE, pairedDeviceActions(managed, false))
        assertEquals(PairedDeviceActions.MORE, pairedDeviceActions(managed, true))
        PairingProjection.starting(peer.id)
        PairingProjection.canceled(PairingProgressNotice(peer.id, peer.name))
        assertEquals(PairedDeviceActions.MORE, pairedDeviceActions(peer, false))
    }
}
