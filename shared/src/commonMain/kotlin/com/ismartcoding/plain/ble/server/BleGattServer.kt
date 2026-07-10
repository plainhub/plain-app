package com.ismartcoding.plain.ble.server

interface BleGattServer {
    fun start()
    fun stop()
    fun refreshAdvertising()

    fun sendNotification(mac: String, charUuid: String, value: String): Boolean
}
