package com.ismartcoding.plain.discover

import com.ismartcoding.plain.helpers.JsonHelper
import com.ismartcoding.plain.data.DPairingCancel
import com.ismartcoding.plain.data.DPairingRequest
import com.ismartcoding.plain.data.DPairingResponse
import com.ismartcoding.plain.enums.NearbyMessageType

object PairingMessenger {
    fun sendRequest(request: DPairingRequest, targetIp: String) {
        send(NearbyMessageType.PAIR_REQUEST, JsonHelper.jsonEncode(request), targetIp)
    }

    fun sendResponse(response: DPairingResponse, targetIp: String) {
        send(NearbyMessageType.PAIR_RESPONSE, JsonHelper.jsonEncode(response), targetIp)
    }

    fun sendCancel(cancel: DPairingCancel, targetIp: String) {
        send(NearbyMessageType.PAIR_CANCEL, JsonHelper.jsonEncode(cancel), targetIp)
    }

    private fun send(type: NearbyMessageType, message: String, targetIp: String) {
        NearbyNetwork.sendUnicast("${type.toPrefix()}$message", targetIp)
    }
}
