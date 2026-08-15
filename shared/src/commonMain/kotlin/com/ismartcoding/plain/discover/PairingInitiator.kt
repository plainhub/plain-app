package com.ismartcoding.plain.discover

import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.data.DNearbyDevice
import com.ismartcoding.plain.data.DPairingCancel
import com.ismartcoding.plain.data.DPairingResult
import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.helpers.JsonHelper
import com.ismartcoding.plain.helpers.coIO
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.platform.getBestIp
import com.ismartcoding.plain.ui.models.NearbyViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object PairingInitiator {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    suspend fun start(device: DNearbyDevice) = withIO {
        try {
            val bestIp = getBestIp(device.ips)
            val request = PairingCore.startPairingSession(device, bestIp)
            if (PairingMessenger.sendRequest(request, bestIp, device.port)) {
                NearbyViewModel.onPairingRequestSent(device.id)
                sendEvent(
                    WebSocketEvent(
                        EventType.PAIRING_STARTED,
                        JsonHelper.jsonEncode(DPairingResult(deviceId = device.id, deviceName = device.name)),
                    )
                )
                awaitPairResponse(device)
            } else {
                PairingSessionStore.remove(device.id)
                PairingCore.notifyFailed(device.id, device.name, "Failed to send pairing request")
            }
        } catch (e: Exception) {
            LogCat.e("[Pairing] Error starting pairing: ${e.message}")
            PairingSessionStore.remove(device.id)
            PairingCore.notifyFailed(device.id, device.name, "Failed to send pairing request")
        }
    }

    /**
     * Fails the pairing when the peer never answers. The session is removed
     * by [PairingCore.handlePairResponse] (or [cancel]) on the happy paths,
     * so its continued existence after [PAIR_RESPONSE_TIMEOUT_MS] means no
     * response arrived.
     */
    private fun awaitPairResponse(device: DNearbyDevice) {
        scope.launch {
            delay(PAIR_RESPONSE_TIMEOUT_MS)
            if (PairingSessionStore.get(device.id) != null) {
                PairingSessionStore.remove(device.id)
                LogCat.e("[Pairing] Response timeout after ${PAIR_RESPONSE_TIMEOUT_MS}ms for ${device.name}")
                PairingCore.notifyFailed(device.id, device.name, "Pairing timed out")
            }
        }
    }

    fun cancel(deviceId: String) {
        val session = PairingSessionStore.get(deviceId)
        if (session != null) {
            try {
                val cancelMessage = DPairingCancel(
                    fromId = TempData.clientId,
                    toId = deviceId,
                )
                coIO {
                    PairingMessenger.sendCancel(cancelMessage, session.deviceIp, session.devicePort)
                    sendEvent(
                        WebSocketEvent(
                            EventType.PAIRING_CANCELED,
                            JsonHelper.jsonEncode(DPairingResult(deviceId = deviceId, deviceName = session.deviceName)),
                        )
                    )
                    LogCat.d("Pairing cancel message sent to ${session.deviceName}")
                }
            } catch (e: Exception) {
                LogCat.e("Error sending pairing cancel message: ${e.message}")
            }
        }

        PairingSessionStore.remove(deviceId)

        LogCat.d("Pairing cancelled for device: $deviceId")
    }
}

private const val PAIR_RESPONSE_TIMEOUT_MS = 90_000L
