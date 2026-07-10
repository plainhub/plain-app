package com.ismartcoding.plain.ble.server

import com.ismartcoding.plain.ble.BleRequestData
import com.ismartcoding.plain.ble.BleUuids
import com.ismartcoding.plain.data.DPairingCancel
import com.ismartcoding.plain.data.DPairingRequest
import com.ismartcoding.plain.data.DPairingResponse
import com.ismartcoding.plain.discover.PairingCore
import com.ismartcoding.plain.enums.NearbyMessageType
import com.ismartcoding.plain.helpers.JsonHelper
import com.ismartcoding.plain.lib.logcat.LogCat

class NearbyServiceHandler : BleServiceHandler {
    override val charUuid: String = BleUuids.NEARBY_CHAR_UUID

    override suspend fun handleRequest(requestData: BleRequestData, clientMac: String): String? {
        val body = requestData.body
        val type = NearbyMessageType.entries.firstOrNull { body.startsWith(it.toPrefix()) } ?: run {
            LogCat.e("NearbyServiceHandler: unknown message type, body=${body.take(50)}")
            return null
        }
        val payload = body.removePrefix(type.toPrefix())
        LogCat.d("NearbyServiceHandler: type=$type from=$clientMac")

        return when (type) {
            NearbyMessageType.DISCOVER ->
                JsonHelper.jsonEncode(PairingCore.buildDiscoverReply())

            NearbyMessageType.DISCOVER_REPLY -> null

            NearbyMessageType.PAIR_REQUEST -> {
                PairingCore.handlePairRequest(JsonHelper.jsonDecode(payload), clientMac, isBle = true)
                "1"
            }

            NearbyMessageType.PAIR_RESPONSE -> {
                PairingCore.handlePairResponse(JsonHelper.jsonDecode(payload), senderIp = "")
                "1"
            }

            NearbyMessageType.PAIR_CANCEL -> {
                PairingCore.handlePairCancel(JsonHelper.jsonDecode(payload))
                "1"
            }
        }
    }
}
