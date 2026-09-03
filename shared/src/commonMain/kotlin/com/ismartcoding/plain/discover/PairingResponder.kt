package com.ismartcoding.plain.discover

import com.ismartcoding.plain.ble.BleUuids
import com.ismartcoding.plain.ble.PairingTransport
import com.ismartcoding.plain.ble.server.BlePairingSessionStore
import com.ismartcoding.plain.data.DPairingCancel
import com.ismartcoding.plain.data.DPairingRequest
import com.ismartcoding.plain.data.DPairingResponse
import com.ismartcoding.plain.enums.NearbyMessageType
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.lib.logcat.LogCat

object PairingResponder {

    suspend fun respond(request: DPairingRequest, accepted: Boolean) = withIO {
        val isBle = BlePairingSessionStore.has(request.fromId)
        try {
            val response = if (accepted) {
                PairingCore.acceptPairingRequest(request)
            } else {
                PairingCore.buildRejectionResponse(request)
            }

            if (response == null) {
                PairingCore.notifyFailed(request.fromId, request.fromName, "Failed to ${if (accepted) "accept" else "respond to"} pairing request")
                if (isBle) BlePairingSessionStore.remove(request.fromId)
                return@withIO
            }

            // The same request may be shown in a web client's (plain-desktop)
            // invite modal — broadcast the rejection so it can dismiss itself.
            if (!accepted) {
                PairingCore.notifyFailed(request.fromId, request.fromName, "Pairing request was rejected")
            }

            // Send via both channels simultaneously — the initiator discards the duplicate
            if (request.fromIp.isNotEmpty()) {
                try {
                    if (!PairingMessenger.sendResponse(response, request.fromIp, request.port)) {
                        LogCat.e("LAN response failed for ${request.fromName} (${request.fromId})")
                    }
                } catch (e: Exception) {
                    LogCat.e("LAN response failed: ${e.message}")
                }
            }
            if (isBle) {
                sendResponseViaBle(request.fromId, request.fromName, response)
            }
        } catch (e: Exception) {
            LogCat.e("Error responding to pairing: ${e.message}")
            PairingCore.notifyFailed(request.fromId, request.fromName, "Failed to respond to pairing request")
            if (isBle) BlePairingSessionStore.remove(request.fromId)
        } finally {
            PairingSessionStore.remove(request.fromId)
        }
    }

    private fun sendResponseViaBle(fromId: String, fromName: String, response: DPairingResponse) {
        val mac = BlePairingSessionStore.get(fromId)
        if (mac == null) {
            LogCat.e("sendResponseViaBle: no stored MAC for $fromId")
            return
        }
        BlePairingSessionStore.remove(fromId)

        val body = PairingCore.formatMessage(NearbyMessageType.PAIR_RESPONSE, JsonHelper.jsonEncode(response))
        val sent = PairingTransport.sendNotification(mac, BleUuids.NEARBY_CHAR_UUID, body)
        if (!sent) {
            LogCat.e("sendResponseViaBle: notification failed for mac=$mac")
            PairingCore.notifyFailed(fromId, fromName, "Failed to send pairing response via BLE")
        } else {
            LogCat.d("sendResponseViaBle: response sent via notification to mac=$mac")
        }
    }

    fun onCancel(cancel: DPairingCancel) {
        LogCat.d("Pairing cancelled by remote device: ${cancel.fromId}")
        PairingCore.handlePairCancel(cancel)
    }
}
