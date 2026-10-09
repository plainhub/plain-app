package com.ismartcoding.plain.ble.client

import com.ismartcoding.plain.ble.*
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class BleDeviceApi(val device: BleGattClient, private val wire: BleWire) {
    val id = device.id
    val name: String get() = device.name ?: "unknown"
    fun isConnected(): Boolean = device.isConnected()
    fun disconnect() = device.disconnect()
    suspend fun ensureConnected(retries: Int = 3): Boolean = device.ensureConnected(retries)

    suspend fun sendRequest(service: BleService, message: ByteArray, requestId: Int): Boolean {
        if (!device.setNotification(service, true)) return false
        return writeRequest(service, message, requestId)
    }
    suspend fun requestAsync(service: BleService, message: ByteArray): ByteArray = withTimeout(120_000) {
        val requestId = wire.nextRequestId()
        val assembly = wire.assembler()
        try {
            check(sendRequest(service, message, requestId)) { "BLE request write failed" }
            while (true) {
                val frame = checkNotNull(device.waitForNotification(service, 15_000)) { "BLE notification timed out" }
                val result = assembly.push(frame)
                check(assembly.response && assembly.requestId == requestId) { "Unexpected BLE response" }
                if (result != null) return@withTimeout result
            }
            @Suppress("UNREACHABLE_CODE")
            error("BLE response unavailable")
        } finally {
            assembly.close()
            withContext(NonCancellable) { withTimeout(5_000) { device.setNotification(service, false) } }
        }
    }
    private suspend fun writeRequest(service: BleService, message: ByteArray, requestId: Int): Boolean {
        val encoder = wire.encoder(message, requestId, false, device.maximumWriteValueLength)
        try {
            while (true) {
                val frame = encoder.next() ?: return true
                if (!device.writeCharacteristic(service, frame)) return false
            }
        } finally { encoder.close() }
    }
}
