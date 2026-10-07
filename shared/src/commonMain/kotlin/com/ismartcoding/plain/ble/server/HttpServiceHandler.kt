package com.ismartcoding.plain.ble.server

import com.ismartcoding.plain.ble.BleDataChannel
import com.ismartcoding.plain.ble.BleUuids

class HttpServiceHandler : BleServiceHandler {
    override val charUuid: String = BleUuids.HTTP_CHAR_UUID
    override suspend fun handleRequest(message: ByteArray, clientMac: String): ByteArray =
        BleDataChannel.incoming(clientMac, charUuid, message)
}
