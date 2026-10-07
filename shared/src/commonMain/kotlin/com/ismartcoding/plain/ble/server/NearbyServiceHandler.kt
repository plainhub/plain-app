package com.ismartcoding.plain.ble.server

import com.ismartcoding.plain.ble.RustBleWire
import com.ismartcoding.plain.ble.BleUuids
import com.ismartcoding.plain.discover.RustNearbyWire
import com.ismartcoding.plain.discover.PairingCore
import com.ismartcoding.plain.enums.NearbyMessageType
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.lib.logcat.LogCat

class NearbyServiceHandler : BleServiceHandler {
    override val charUuid: String = BleUuids.NEARBY_CHAR_UUID

    override suspend fun handleRequest(message: ByteArray, clientMac: String): ByteArray {
        val result = handleBody(RustBleWire.nearbyBody(message).decodeToString(throwOnInvalidSequence = true), clientMac)
        return RustBleWire.nearby((result ?: "").encodeToByteArray())
    }

    private suspend fun handleBody(body: String, clientMac: String): String? {
        val message = RustNearbyWire.parse(body) ?: run {
            return null
        }
        val (type, payload) = message
        LogCat.d("NearbyServiceHandler: type=$type from=$clientMac")

        return when (type) {
            NearbyMessageType.DISCOVER ->
                com.ismartcoding.plain.discover.RustDiscoveryAdvertisement.replyJson().toString()

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
