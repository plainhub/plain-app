package com.ismartcoding.plain.discover

import com.ismartcoding.plain.data.DNearbyDevice
import com.ismartcoding.plain.data.DPairingCancel
import com.ismartcoding.plain.data.DPairingRequest
import com.ismartcoding.plain.data.DPairingResponse

object NearbyPairing {

    suspend fun startPairingAsync(device: DNearbyDevice) {
        PairingInitiator.start(device)
    }

    suspend fun respondToPairing(request: DPairingRequest, accepted: Boolean) {
        PairingResponder.respond(request, accepted)
    }

    suspend fun handlePairingResponse(response: DPairingResponse, senderIp: String) {
        PairingInitiator.onResponse(response, senderIp)
    }

    fun cancelPairing(deviceId: String) {
        PairingInitiator.cancel(deviceId)
    }

    fun handlePairingCancel(cancel: DPairingCancel) {
        PairingResponder.onCancel(cancel)
    }
}
