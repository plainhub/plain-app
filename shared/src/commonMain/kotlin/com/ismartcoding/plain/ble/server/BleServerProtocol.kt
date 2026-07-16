package com.ismartcoding.plain.ble.server

import com.ismartcoding.plain.ble.BleRequestData
import com.ismartcoding.plain.ble.BleSegmentData
import com.ismartcoding.plain.platform.PlatformLock
import com.ismartcoding.plain.lib.logcat.LogCat

class BleServerProtocol {
    val handlers = listOf(
        NearbyServiceHandler(),
        HTTPServiceHandler(),
    )
    private val handlerMap = handlers.associateBy { it.charUuid }
    private val pendingRequests = mutableMapOf<String, StringBuilder>()
    private val responses = mutableMapOf<String, MutableMap<String, ByteArray>>()
    private val responseLock = PlatformLock()

    suspend fun handleWrite(mac: String, charUuid: String, value: ByteArray): Boolean {
        val handler = handlerMap[charUuid] ?: run {
            LogCat.e("[GATT] handleWrite mac=$mac charUuid=$charUuid: no handler registered, known=${handlerMap.keys}")
            return false
        }

        val segment = try {
            BleSegmentData.fromJSON(value.decodeToString())
        } catch (e: Exception) {
            LogCat.e("[GATT] handleWrite mac=$mac charUuid=$charUuid: segment parse error: ${e.message}")
            pendingRequests.remove(mac)
            return false
        }

        val buffer = pendingRequests.getOrPut(mac) { StringBuilder() }
        buffer.append(segment.data)
        LogCat.d("[GATT] handleWrite mac=$mac charUuid=$charUuid: segment dataLen=${segment.data.length} isEnd=${segment.isEnd()} bufferLen=${buffer.length}")

        if (!segment.isEnd()) return false

        val requestJson = buffer.toString()
        pendingRequests.remove(mac)
        LogCat.d("[GATT] handleWrite mac=$mac charUuid=$charUuid: full request received, jsonLen=${requestJson.length}")

        val responseData = try {
            val requestData = BleRequestData.fromJSON(requestJson)
            handler.handleRequest(requestData, mac)
        } catch (e: Exception) {
            LogCat.e("[GATT] handleWrite mac=$mac charUuid=$charUuid: handler error: ${e.message}")
            null
        }

        val responseBytes = (responseData ?: "").encodeToByteArray()
        LogCat.d("[GATT] handleWrite mac=$mac charUuid=$charUuid: response size=${responseBytes.size}")
        responseLock.withLock {
            responses.getOrPut(mac) { mutableMapOf() }[charUuid] = responseBytes
        }

        return true
    }

    fun handleRead(mac: String, charUuid: String, offset: Int): ByteArray {
        val payload = responseLock.withLock {
            responses[mac]?.get(charUuid)
        } ?: return ByteArray(0)
        return if (offset < payload.size) payload.copyOfRange(offset, payload.size) else ByteArray(0)
    }

    fun clearClient(mac: String) {
        pendingRequests.remove(mac)
        responseLock.withLock {
            responses.remove(mac)
        }
    }
}
