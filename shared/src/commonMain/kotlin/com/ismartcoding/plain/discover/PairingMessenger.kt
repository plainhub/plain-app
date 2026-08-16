package com.ismartcoding.plain.discover

import com.ismartcoding.plain.data.DPairingCancel
import com.ismartcoding.plain.data.DPairingRequest
import com.ismartcoding.plain.data.DPairingResponse
import com.ismartcoding.plain.enums.NearbyMessageType
import com.ismartcoding.plain.lib.JsonHelper

/**
 * Delivers pairing messages to a peer's `POST /nearby` endpoint over the LAN.
 *
 * The request body is the same [NearbyMessageType]-prefixed wire format the
 * BLE nearby service uses ("PAIR_REQUEST:{…}" — see
 * [PairingCore.formatMessage]).
 */
object PairingMessenger {
    suspend fun sendRequest(request: DPairingRequest, targetIp: String, targetPort: Int): Boolean =
        send(NearbyMessageType.PAIR_REQUEST, JsonHelper.jsonEncode(request), targetIp, targetPort)

    suspend fun sendResponse(response: DPairingResponse, targetIp: String, targetPort: Int): Boolean =
        send(NearbyMessageType.PAIR_RESPONSE, JsonHelper.jsonEncode(response), targetIp, targetPort)

    suspend fun sendCancel(cancel: DPairingCancel, targetIp: String, targetPort: Int): Boolean =
        send(NearbyMessageType.PAIR_CANCEL, JsonHelper.jsonEncode(cancel), targetIp, targetPort)

    private suspend fun send(type: NearbyMessageType, json: String, targetIp: String, targetPort: Int): Boolean {
        return NearbyHttpClient.post(PairingCore.formatMessage(type, json), targetIp, targetPort)
    }
}
