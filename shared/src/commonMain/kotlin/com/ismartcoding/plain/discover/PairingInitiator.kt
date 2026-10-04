package com.ismartcoding.plain.discover

import com.ismartcoding.plain.data.DNearbyDevice
import com.ismartcoding.plain.data.DPairingTicket
import com.ismartcoding.plain.lib.coIO
import com.ismartcoding.plain.lib.withIO
import kotlinx.coroutines.*

object PairingInitiator {

    suspend fun start(device: DNearbyDevice) = withIO { RustPairingRuntime.startLan(device) }

    suspend fun fail(ticket: DPairingTicket, reason: String) {
        RustPairingStore.cancel(ticket.deviceId, ticket.generation)?.let {
            PairingCore.notifyFailed(it.first.deviceId, it.first.deviceName, reason)
        }
    }

    fun cancel(deviceId: String) { coIO { RustPairingRuntime.cancel(deviceId) } }
}
