package com.ismartcoding.plain.ble.client

import com.ismartcoding.plain.ble.BleActionResult
import com.ismartcoding.plain.ble.BleRequestData
import com.ismartcoding.plain.ble.BleResult
import com.ismartcoding.plain.ble.BleSegmentData
import com.ismartcoding.plain.ble.BleService
import com.ismartcoding.plain.helpers.JsonHelper
import com.ismartcoding.plain.lib.logcat.LogCat

class BleDeviceApi(val device: BleGattClient) {
    val id = device.id
    val name: String get() = device.name ?: "unknown"

    fun isConnected(): Boolean = device.isConnected()

    fun disconnect() {
        LogCat.d("Disconnecting from ${device.id}")
        device.disconnect()
    }

    suspend fun ensureConnected(retries: Int = 3): Boolean {
        return device.ensureConnected(retries)
    }

    suspend fun sendRequest(
        service: BleService,
        requestData: BleRequestData,
    ): Boolean {
        LogCat.d("[BLE] sendRequest ${service.name} ${device.id} start, connected=${device.isConnected()}")
        return writeRequest(service, requestData)
    }

    suspend fun requestAsync(
        service: BleService,
        requestData: BleRequestData,
    ): BleResult {
        val tag = "[BLE] requestAsync ${service.name} ${device.id}"
        LogCat.d("$tag start, connected=${device.isConnected()}")

        if (!writeRequest(service, requestData)) {
            return BleResult(service.charUuid, null, BleActionResult.FAIL)
        }

        LogCat.d("$tag all chunks written, waiting for ready notification")
        val notifyResult = device.waitForNotification(service, NOTIFY_TIMEOUT_MS)
        LogCat.d("$tag waitForNotification=$notifyResult")
        if (notifyResult == null) {
            LogCat.e("$tag TIMEOUT: no ready notification within ${NOTIFY_TIMEOUT_MS}ms")
            device.setNotification(service, false)
            return BleResult(service.charUuid, null, BleActionResult.TIMEOUT)
        }

        val readResult = device.readCharacteristic(service)
        LogCat.d("$tag readCharacteristic=${readResult?.length}")
        device.setNotification(service, false)

        return if (!readResult.isNullOrEmpty()) {
            LogCat.d("$tag SUCCESS")
            BleResult(service.charUuid, readResult, BleActionResult.SUCCESS)
        } else {
            LogCat.e("$tag FAIL: empty response")
            BleResult(service.charUuid, null, BleActionResult.FAIL)
        }
    }

    private suspend fun writeRequest(service: BleService, requestData: BleRequestData): Boolean {
        val tag = "[BLE] writeRequest ${service.name} ${device.id}"
        val r = device.setNotification(service, true)
        LogCat.d("$tag setNotification(true)=$r connected=${device.isConnected()}")
        if (!r) {
            LogCat.e("$tag FAIL: setNotification(true) returned false")
            return false
        }

        val requestJson = JsonHelper.jsonEncode(requestData)
        val chunks = requestJson.chunked(CHUNK_SIZE)
        LogCat.d("$tag sending $requestData")
        chunks.forEachIndexed { index, chunk ->
            val segment = BleSegmentData.build(
                chunk,
                start = index == 0,
                end = index == chunks.lastIndex,
            )
            val wr = device.writeCharacteristic(service, JsonHelper.jsonEncode(segment))
            if (!wr) {
                LogCat.e("$tag FAIL: writeCharacteristic chunk $index/${chunks.size} returned false, connected=${device.isConnected()}")
                device.setNotification(service, false)
                return false
            }
        }
        return true
    }

    companion object {
        private const val CHUNK_SIZE = 380
        private const val NOTIFY_TIMEOUT_MS = 15_000L
    }
}
