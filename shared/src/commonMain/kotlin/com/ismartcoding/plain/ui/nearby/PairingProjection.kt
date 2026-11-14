package com.ismartcoding.plain.ui.nearby

import com.ismartcoding.plain.features.chat.peer.PeerStatusClient
import com.ismartcoding.plain.features.chat.peer.RustPeerStore
import com.ismartcoding.plain.models.PairingRequestNotice
import com.ismartcoding.plain.models.PairingProgressNotice
import com.ismartcoding.plain.ui.app.MainViewModel

object PairingProjection {
    fun starting(peerId: String) {
        NearbyViewModel.pairingViews[peerId] = PairingView(peerId, PairingStep.PAIRING)
        NearbyViewModel.itemStatus[peerId] = NearbyItemStatus.STARTING
    }
    fun started(notice: PairingProgressNotice) {
        val current = NearbyViewModel.pairingViews[notice.deviceId] ?: PairingView(notice.deviceId, PairingStep.PAIRING)
        NearbyViewModel.pairingViews[notice.deviceId] = current.copy(step = PairingStep.PAIRING, verificationCode = notice.verificationCode)
        NearbyViewModel.itemStatus[notice.deviceId] = NearbyItemStatus.PAIRING
    }
    fun failed(notice: PairingProgressNotice) {
        val step = if (notice.error in setOf("password_required", "invalid_password")) PairingStep.PASSWORD_REQUIRED else PairingStep.FAILED
        NearbyViewModel.pairingViews[notice.deviceId] = PairingView(notice.deviceId, step, notice.error)
        NearbyViewModel.itemStatus.remove(notice.deviceId)
    }
    fun canceled(notice: PairingProgressNotice) {
        NearbyViewModel.pairingViews[notice.deviceId] = PairingView(notice.deviceId, PairingStep.CANCELED)
        NearbyViewModel.itemStatus.remove(notice.deviceId)
    }
    fun completed(notice: PairingProgressNotice) {
        NearbyViewModel.pairingViews[notice.deviceId] = PairingView(notice.deviceId, PairingStep.SUCCEEDED)
        NearbyViewModel.handlePairingSuccess(notice.deviceId)
    }
    fun request(notice: PairingRequestNotice) {
        val current = NearbyViewModel.pairingViews[notice.fromId]
        if (current?.step == PairingStep.PAIRING) {
            NearbyViewModel.pairingViews[notice.fromId] = current.copy(step = PairingStep.TRUST_REQUIRED,
                verificationCode = notice.verificationCode, identityChanged = notice.identityChanged)
        } else { MainViewModel.pendingPairingRequest.value = notice }
    }
    internal suspend fun success(deviceId: String, key: String) {
        val peer = RustPeerStore.getById(deviceId) ?: return
        if (!peer.isPaired() || peer.key != key) return
        NearbyViewModel.handlePairingSuccess(peer.id)
        PeerStatusClient.reconnectNow("post_pairing")
    }
}
