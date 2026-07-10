package com.ismartcoding.plain.discover

import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.data.DNearbyDevice
import com.ismartcoding.plain.data.DPairingCancel
import com.ismartcoding.plain.lib.helpers.NetworkHelper
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.helpers.withIO

object PairingInitiator {

    suspend fun start(device: DNearbyDevice) = withIO {
        try {
            val bestIp = NetworkHelper.getBestIp(device.ips)
            val request = PairingCore.startPairingSession(device, bestIp)
            PairingMessenger.sendRequest(request, bestIp)
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
