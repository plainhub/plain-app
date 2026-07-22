package com.ismartcoding.plain.discover

import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.data.DNearbyDevice
import com.ismartcoding.plain.data.DPairingCancel
import com.ismartcoding.plain.data.DPairingResult
import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.helpers.JsonHelper
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.lib.channel.sendEvent
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.platform.getBestIp

object PairingInitiator {
    suspend fun start(device: DNearbyDevice) = withIO {
        try {
            val bestIp = getBestIp(device.ips)
            val request = PairingCore.startPairingSession(device, bestIp)
            PairingMessenger.sendRequest(request, bestIp)
            sendEvent(
                WebSocketEvent(
                    EventType.PAIRING_STARTED,
                    JsonHelper.jsonEncode(DPairingResult(deviceId = device.id, deviceName = device.name)),
                )
            )
        } catch (e: Exception) {
            LogCat.e("[Pairing] Error starting pairing: ${e.message}")
            PairingCore.notifyFailed(device.id, device.name, "Failed to send pairing request")
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
                PairingMessenger.sendCancel(cancelMessage, session.deviceIp)
                sendEvent(
                    WebSocketEvent(
                        EventType.PAIRING_CANCELED,
                        JsonHelper.jsonEncode(DPairingResult(deviceId = deviceId, deviceName = session.deviceName ?: "")),
                    )
                )
                LogCat.d("Pairing cancel message sent to ${session.deviceName}")
            } catch (e: Exception) {
                LogCat.e("Error sending pairing cancel message: ${e.message}")
            }
        }

        PairingSessionStore.remove(deviceId)

        LogCat.d("Pairing cancelled for device: $deviceId")
    }
}
