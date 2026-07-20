package com.ismartcoding.plain.discover

import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.data.DNearbyDevice
import com.ismartcoding.plain.data.DPairingCancel
import com.ismartcoding.plain.helpers.TimeHelper
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.platform.getBestIp
import kotlinx.coroutines.delay

object PairingInitiator {
    private const val PAIRING_TIMEOUT_MS = 90_000L
    private const val PAIRING_RETRY_INTERVAL_MS = 3_000L

    suspend fun start(device: DNearbyDevice) = withIO {
        try {
            val bestIp = getBestIp(device.ips)
            val request = PairingCore.startPairingSession(device, bestIp)
            // LAN pairing uses UDP unicast (fire-and-forget). A single packet
            // can be lost on busy Wi-Fi, so retransmit the request until the
            // session is resolved (success/fail/cancel removes it from the
            // store) or the 90s deadline elapses. Without this, a single lost
            // datagram caused the full 90s timeout.
            val deadline = TimeHelper.nowMillis() + PAIRING_TIMEOUT_MS
            while (TimeHelper.nowMillis() < deadline &&
                PairingSessionStore.get(device.id) != null
            ) {
                PairingMessenger.sendRequest(request, bestIp)
                delay(PAIRING_RETRY_INTERVAL_MS)
            }
        } catch (e: Exception) {
            LogCat.e("[Pairing] Error starting pairing: ${e.message}")
            PairingCore.notifyFailed(device.id, device.name, "Failed to send pairing request")
        } finally {
            PairingSessionStore.remove(device.id)
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
                LogCat.d("Pairing cancel message sent to ${session.deviceName}")
            } catch (e: Exception) {
                LogCat.e("Error sending pairing cancel message: ${e.message}")
            }
        }

        PairingSessionStore.remove(deviceId)
        PairingCore.notifyFailed(deviceId, session?.deviceName ?: "", "Pairing cancelled by user")
        LogCat.d("Pairing cancelled for device: $deviceId")
    }
}
