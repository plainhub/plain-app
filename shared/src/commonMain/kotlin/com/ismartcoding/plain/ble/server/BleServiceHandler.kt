package com.ismartcoding.plain.ble.server

interface BleServiceHandler {
    val charUuid: String
    suspend fun handleRequest(message: ByteArray, clientMac: String): ByteArray
}
