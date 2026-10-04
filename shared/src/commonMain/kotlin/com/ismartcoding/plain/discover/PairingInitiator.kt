package com.ismartcoding.plain.discover

import com.ismartcoding.plain.data.DNearbyDevice
import com.ismartcoding.plain.data.DPairingResult
import com.ismartcoding.plain.data.DPairingTicket
import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.lib.coIO
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.platform.getBestIp
import com.ismartcoding.plain.ui.models.NearbyViewModel
import kotlinx.coroutines.*

object PairingInitiator {

    suspend fun start(device: DNearbyDevice) = withIO {
        var ticket: DPairingTicket? = null
        try {
            val (request, started) = PairingCore.startPairingSession(device, getBestIp(device.ips))
            ticket = started
            if (PairingMessenger.sendRequest(request, started.deviceIp, started.devicePort)) {
                NearbyViewModel.onPairingRequestSent(device.id)
                sendEvent(WebSocketEvent(EventType.PAIRING_STARTED, JsonHelper.jsonEncode(DPairingResult(deviceId = device.id, deviceName = device.name))))
            } else {
                fail(started, "Failed to send pairing request")
            }
        } catch (e: CancellationException) {
            ticket?.let { withContext(NonCancellable) { RustPairingStore.cancel(it.deviceId, it.generation) } }
            throw e
        } catch (e: Exception) {
            LogCat.e("[Pairing] Error starting pairing: ${e.message}")
            val started = ticket
            if (started == null) PairingCore.notifyFailed(device.id, device.name, "Failed to send pairing request")
            else fail(started, "Failed to send pairing request")
        }
    }

    suspend fun fail(ticket: DPairingTicket, reason: String) {
        RustPairingStore.cancel(ticket.deviceId, ticket.generation)?.let {
            PairingCore.notifyFailed(it.first.deviceId, it.first.deviceName, reason)
        }
    }

    fun cancel(deviceId: String) {
        coIO {
            val (ticket, message) = RustPairingStore.cancel(deviceId) ?: return@coIO
            sendEvent(WebSocketEvent(EventType.PAIRING_CANCELED, JsonHelper.jsonEncode(DPairingResult(deviceId = ticket.deviceId, deviceName = ticket.deviceName))))
            try { PairingMessenger.sendCancel(message, ticket.deviceIp, ticket.devicePort) }
            catch (e: Exception) { LogCat.e("Error sending pairing cancel message: ${e.message}") }
        }
    }
}
