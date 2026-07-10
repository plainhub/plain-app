package com.ismartcoding.plain.ble

import com.ismartcoding.plain.ble.client.AndroidBleScanner
import com.ismartcoding.plain.ble.client.BleScanner
import com.ismartcoding.plain.ble.server.AndroidBleGattServer
import com.ismartcoding.plain.ble.server.BleGattServer

actual fun bleTransport(): BleTransport = AndroidBleTransport

object AndroidBleTransport : BleTransport {
    override fun createScanner(): BleScanner = AndroidBleScanner

    override fun createServer(): BleGattServer = AndroidBleGattServer()
}
