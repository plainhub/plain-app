package com.ismartcoding.plain.ble.client

import com.ismartcoding.plain.ble.BleActionResult
import com.ismartcoding.plain.ble.BleRequestData
import com.ismartcoding.plain.ble.BleResult
import com.ismartcoding.plain.ble.BleSegmentData
import com.ismartcoding.plain.ble.BleService
import com.ismartcoding.plain.lib.JsonHelper
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

    /**
     * Send an RPC request and receive the response via chunked notifications.
     *
     * The server splits the response into [BleSegmentData] segments and
     * pushes each as a notification with flow control. This bypasses the
     * 512-byte ATT attribute value limit that truncated large
     * `readCharacteristic` responses (e.g. base64-encoded `/fs` file chunks).
     *
     * Flow:
     *  1. Enable notifications.
     *  2. Write the request in chunks (existing [writeRequest] path).
     *  3. Receive [BleSegmentData] notifications until the end segment.
     *  4. Reassemble into the full response JSON.
     *  5. Disable notifications.
     */
    suspend fun requestAsync(
        service: BleService,
        requestData: BleRequestData,
    ): BleResult {
        val tag = "[BLE] requestAsync ${service.name} ${device.id}"
        LogCat.d("$tag start, connected=${device.isConnected()}")

        if (!writeRequest(service, requestData)) {
            device.setNotification(service, false)
            return BleResult(service.charUuid, null, BleActionResult.FAIL)
        }

        LogCat.d("$tag all chunks written, receiving chunked response")
        val responseBuilder = StringBuilder()
        var chunkCount = 0
        while (true) {
            val notifyResult = device.waitForNotification(service, NOTIFY_TIMEOUT_MS)
            if (notifyResult == null) {
                LogCat.e("$tag TIMEOUT: no notification within ${NOTIFY_TIMEOUT_MS}ms (received $chunkCount chunks, ${responseBuilder.length} bytes)")
                device.setNotification(service, false)
                return BleResult(service.charUuid, null, BleActionResult.TIMEOUT)
            }

            val segment = try {
                BleSegmentData.fromJSON(notifyResult)
            } catch (e: Exception) {
                LogCat.e("$tag failed to parse segment: ${e.message}, raw=${notifyResult.take(200)}")
                device.setNotification(service, false)
                return BleResult(service.charUuid, null, BleActionResult.FAIL)
            }
            responseBuilder.append(segment.data)
            chunkCount++
            if (chunkCount % 20 == 0) {
                LogCat.d("$tag received $chunkCount chunks, ${responseBuilder.length} bytes so far, isEnd=${segment.isEnd()}")
            }
            if (segment.isEnd()) break
        }

        device.setNotification(service, false)
        val responseJson = responseBuilder.toString()
        LogCat.d("$tag SUCCESS: $chunkCount chunks, ${responseJson.length} bytes")

        return BleResult(service.charUuid, responseJson, BleActionResult.SUCCESS)
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
