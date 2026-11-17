package com.ismartcoding.plain.ui.nearby

import com.ismartcoding.plain.models.PairingErrorCode

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
        val current = NearbyViewModel.pairingViews[notice.deviceId] ?: return
        if (current.step != PairingStep.PAIRING && current.step != PairingStep.WAITING_FOR_PEER) return
        NearbyViewModel.pairingViews[notice.deviceId] =
            PairingView(notice.deviceId, PairingStep.WAITING_FOR_PEER, verificationCode = notice.verificationCode)
        NearbyViewModel.itemStatus[notice.deviceId] = NearbyItemStatus.PAIRING
    }
    fun trustAccepted(peerId: String) {
        if (NearbyViewModel.pairingViews[peerId]?.step != PairingStep.TRUST_REQUIRED) return
        NearbyViewModel.pairingViews[peerId] = PairingView(peerId, PairingStep.WAITING_FOR_PEER)
    }
    fun failed(notice: PairingProgressNotice) {
        val step = if (notice.error in setOf(PairingErrorCode.PASSWORD_REQUIRED, PairingErrorCode.INVALID_PASSWORD)) PairingStep.PASSWORD_REQUIRED else PairingStep.FAILED
        NearbyViewModel.pairingViews[notice.deviceId] = PairingView(notice.deviceId, step, notice.error)
        NearbyViewModel.itemStatus.remove(notice.deviceId)
    }
    fun canceled(notice: PairingProgressNotice) {
        if (MainViewModel.pendingPairingRequest.value?.fromId == notice.deviceId) {
            MainViewModel.pendingPairingRequest.value = null
        }
        NearbyViewModel.pairingViews[notice.deviceId] = PairingView(notice.deviceId, PairingStep.CANCELED)
        NearbyViewModel.itemStatus.remove(notice.deviceId)
    }
    fun completed(notice: PairingProgressNotice) {
        NearbyViewModel.pairingViews[notice.deviceId] = PairingView(notice.deviceId, PairingStep.SUCCEEDED)
        NearbyViewModel.handlePairingSuccess(notice.deviceId)
    }
    fun request(notice: PairingRequestNotice) {
        val current = NearbyViewModel.pairingViews[notice.fromId]
        when (current?.step) {
            PairingStep.PAIRING -> NearbyViewModel.pairingViews[notice.fromId] = current.copy(
                step = PairingStep.TRUST_REQUIRED,
                verificationCode = notice.verificationCode,
                identityChanged = notice.identityChanged,
            )
            PairingStep.TRUST_REQUIRED, PairingStep.WAITING_FOR_PEER -> Unit
            else -> MainViewModel.pendingPairingRequest.value = notice
        }
    }
    internal suspend fun success(deviceId: String, key: String) {
        val peer = RustPeerStore.getById(deviceId) ?: return
        if (!peer.isPaired() || peer.key != key) return
        NearbyViewModel.handlePairingSuccess(peer.id)
        PeerStatusClient.reconnectNow("post_pairing")
    }
}
