package com.ismartcoding.plain.platform

import com.ismartcoding.plain.ble.client.BleScanner
import com.ismartcoding.plain.ble.client.IosBleScanner
import com.ismartcoding.plain.ble.server.BleGattServer
import com.ismartcoding.plain.ble.server.IosBleGattServer

actual fun isBluetoothEnabled(): Boolean = true

actual fun isBluetoothSupported(): Boolean = true

actual fun isBleReady(): Boolean = false

actual fun isBluetoothReadyToUse(): Boolean = false

actual suspend fun ensureBlePermissionAsync(): Boolean = false

actual fun isBluetoothAdvertiseReady(): Boolean = false

actual fun setBluetoothCanContinue(value: Boolean) {}

actual fun bleTransport(): BleTransport = IosBleTransport

object IosBleTransport : BleTransport {
    override fun createScanner(): BleScanner = IosBleScanner

    override fun createServer(): BleGattServer = IosBleGattServer()
}
