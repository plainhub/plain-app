package com.ismartcoding.plain.ble.server

import com.ismartcoding.plain.ble.*
import com.ismartcoding.plain.platform.PlatformLock
import kotlinx.coroutines.*

class BleServerProtocol {
    val handlers = listOf(NearbyServiceHandler(), HttpServiceHandler())
    private val handlerMap = handlers.associateBy { it.charUuid.lowercase() }
    private val lock = PlatformLock()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val pending = mutableMapOf<Pair<String, String>, BleAssembler>()
    private val expiry = mutableMapOf<Pair<String, String>, Job>()
    private val nearbyIds = mutableMapOf<String, Int>()

    suspend fun handleWrite(mac: String, charUuid: String, value: ByteArray): BleMessage? {
        val key = mac to charUuid.lowercase()
        val handler = checkNotNull(handlerMap[key.second]) { "Unknown BLE characteristic" }
        val request = lock.withLock {
            val assembler = pending.getOrPut(key) {
                check(pending.keys.count { it.first == mac } < 2) { "BLE assembly capacity exceeded" }
                RustBleWire.assembler()
            }
            try {
                val bytes = assembler.push(value)
                check(!assembler.response) { "Unexpected BLE request direction" }
                expiry.remove(key)?.cancel()
                if (bytes == null) {
                    expiry[key] = scope.launch {
                        delay(15_000)
                        lock.withLock { if (pending[key] === assembler) { pending.remove(key)?.close(); expiry.remove(key) } }
                    }
                    null
                } else {
                    val requestId = assembler.requestId
                    pending.remove(key)?.close()
                    if (key.second == BleUuids.NEARBY_CHAR_UUID.lowercase()) nearbyIds[mac] = requestId
                    BleMessage(requestId, bytes)
                }
            } catch (error: Throwable) {
                pending.remove(key)?.close()
                expiry.remove(key)?.cancel()
                throw error
            }
        } ?: return null
        val response = handler.handleRequest(request.bytes, mac)
        return BleMessage(request.requestId, response)
    }
    fun nearbyRequestId(mac: String): Int? = lock.withLock { nearbyIds[mac] }
    fun clearClient(mac: String) = lock.withLock {
        pending.keys.filter { it.first == mac }.forEach { key -> pending.remove(key)?.close(); expiry.remove(key)?.cancel() }
        nearbyIds.remove(mac)
    }
    fun clear() = lock.withLock {
        pending.values.forEach { it.close() }; pending.clear()
        expiry.values.forEach { it.cancel() }; expiry.clear(); nearbyIds.clear()
    }
}
