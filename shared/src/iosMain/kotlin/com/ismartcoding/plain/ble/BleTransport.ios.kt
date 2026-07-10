package com.ismartcoding.plain.ble

import com.ismartcoding.plain.ble.client.BleScanner
import com.ismartcoding.plain.ble.client.IosBleScanner
import com.ismartcoding.plain.ble.server.BleGattServer
import com.ismartcoding.plain.ble.server.IosBleGattServer

actual fun bleTransport(): BleTransport = IosBleTransport

object IosBleTransport : BleTransport {
    override fun createScanner(): BleScanner = IosBleScanner

    override fun createServer(): BleGattServer = IosBleGattServer()
}
