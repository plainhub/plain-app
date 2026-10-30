package com.ismartcoding.plain.features.nearby

import com.ismartcoding.plain.features.chat.peer.PeerStatusClient
import com.ismartcoding.plain.features.chat.peer.RustPeerStore
import com.ismartcoding.plain.models.PairingRequestNotice
import com.ismartcoding.plain.ui.app.MainViewModel
import com.ismartcoding.plain.ui.nearby.NearbyItemStatus
import com.ismartcoding.plain.ui.nearby.NearbyViewModel

object PairingProjection {
    suspend fun reconcile() {
        val before = NearbyViewModel.pairingStates.toMap()
        applySnapshot(before, RustPairingRuntime.states())
        MainViewModel.pendingPairingRequest.value?.let { request ->
            if (!RustPairingRuntime.currentRequest(request.fromId, request.signature) && MainViewModel.pendingPairingRequest.value == request) MainViewModel.pendingPairingRequest.value = null
        }
    }

    internal fun applySnapshot(before: Map<String, PairingState>, states: List<PairingState>) {
        val generations = states.map { it.generation }.toSet()
        val removed = mutableSetOf<String>()
        before.forEach { (generation, state) ->
            if (generation !in generations && NearbyViewModel.pairingStates[generation] == state) {
                NearbyViewModel.pairingStates.remove(generation)
                state.deviceId?.let(removed::add)
            }
        }
        states.forEach(::applyState)
        removed.forEach { id ->
            if (NearbyViewModel.pairingStates.values.none { it.deviceId == id } && NearbyViewModel.itemStatus[id] in setOf(NearbyItemStatus.STARTING, NearbyItemStatus.PAIRING)) NearbyViewModel.itemStatus.remove(id)
        }
    }

    fun applyState(state: PairingState) {
        val id = state.deviceId ?: return
        val generation = state.generation
        val previous = NearbyViewModel.pairingStates[generation]?.phase
        val next = state.phase
        if (previous != null && next != PairingPhase.UNKNOWN && previous.ordinal > next.ordinal) return
        NearbyViewModel.pairingStates[generation] = state
        val active = NearbyViewModel.pairingStates.values.firstOrNull {
            it.deviceId == id && it.phase !in terminalPhases
        }
        NearbyViewModel.itemStatus[id] = when ((active ?: state).phase) {
            PairingPhase.SUCCEEDED -> NearbyItemStatus.PAIRED
            PairingPhase.FAILED, PairingPhase.CANCELLED -> NearbyItemStatus.UNPAIRED
            PairingPhase.STARTING -> NearbyItemStatus.STARTING
            else -> NearbyItemStatus.PAIRING
        }
    }

    suspend fun request(request: PairingRequestNotice) {
        if (!RustPairingRuntime.currentRequest(request.fromId, request.signature)) return
        MainViewModel.pendingPairingRequest.value = request
    }
    private val terminalPhases = setOf(PairingPhase.SUCCEEDED, PairingPhase.FAILED, PairingPhase.CANCELLED)

    internal suspend fun success(deviceId: String, key: String) {
        val peer = RustPeerStore.getById(deviceId) ?: return
        if (!peer.isPaired() || peer.key != key) return
        NearbyViewModel.handlePairingSuccess(peer.id)
        PeerStatusClient.reconnectNow("post_pairing")
    }
}
