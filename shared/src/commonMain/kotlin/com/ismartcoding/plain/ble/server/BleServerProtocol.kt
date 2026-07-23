package com.ismartcoding.plain.ble.server

import com.ismartcoding.plain.ble.BleRequestData
import com.ismartcoding.plain.ble.BleSegmentData
import com.ismartcoding.plain.platform.PlatformLock
import com.ismartcoding.plain.lib.logcat.LogCat

class BleServerProtocol {
    val handlers = listOf(
        NearbyServiceHandler(),
        HttpServiceHandler(),
    )
    private val handlerMap = handlers.associateBy { it.charUuid }
    private val pendingRequests = mutableMapOf<String, StringBuilder>()
    private val pendingLock = PlatformLock()

    /**
     * Process a write segment from the client. Returns the full response
     * string when the last segment of a request has been received and the
     * handler has produced a reply, or null when more segments are expected.
     *
     * The response is delivered back to the [BleGattServer] implementation,
     * which chunks it into [BleSegmentData] notifications. This avoids the
     * 512-byte ATT attribute value limit that truncates large
     * `readCharacteristic` responses (e.g. base64-encoded file chunks from
     * `/fs`).
     */
    suspend fun handleWrite(mac: String, charUuid: String, value: ByteArray): String? {
        val handler = handlerMap[charUuid] ?: run {
            LogCat.e("[GATT] handleWrite mac=$mac charUuid=$charUuid: no handler registered, known=${handlerMap.keys}")
            return null
        }

        val segment = try {
            BleSegmentData.fromJSON(value.decodeToString())
        } catch (e: Exception) {
            LogCat.e("[GATT] handleWrite mac=$mac charUuid=$charUuid: segment parse error: ${e.message}")
            pendingLock.withLock { pendingRequests.remove(mac) }
            return null
        }

        val buffer = pendingLock.withLock { pendingRequests.getOrPut(mac) { StringBuilder() } }
        buffer.append(segment.data)
        LogCat.d("[GATT] handleWrite mac=$mac charUuid=$charUuid: segment dataLen=${segment.data.length} isEnd=${segment.isEnd()} bufferLen=${buffer.length}")

        if (!segment.isEnd()) return null

        val requestJson = buffer.toString()
        pendingLock.withLock { pendingRequests.remove(mac) }
        LogCat.d("[GATT] handleWrite mac=$mac charUuid=$charUuid: full request received, jsonLen=${requestJson.length}")

        val responseData = try {
            val requestData = BleRequestData.fromJSON(requestJson)
            handler.handleRequest(requestData, mac)
        } catch (e: Exception) {
            LogCat.e("[GATT] handleWrite mac=$mac charUuid=$charUuid: handler error: ${e.message}")
            null
        }

        val response = responseData ?: ""
        LogCat.d("[GATT] handleWrite mac=$mac charUuid=$charUuid: response size=${response.length}")
        return response
    }

    fun clearClient(mac: String) {
        pendingLock.withLock { pendingRequests.remove(mac) }
    }
}
