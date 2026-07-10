package com.ismartcoding.plain.ble

import com.ismartcoding.plain.ble.client.BleScanner
import com.ismartcoding.plain.ble.server.BleGattServer

interface BleTransport {
    fun createScanner(): BleScanner

    fun createServer(): BleGattServer
}

expect fun bleTransport(): BleTransport
