package com.ismartcoding.plain.ble.server

import com.ismartcoding.plain.ble.BleRequestData

interface BleServiceHandler {
    val charUuid: String

    suspend fun handleRequest(requestData: BleRequestData, clientMac: String): String?
}
