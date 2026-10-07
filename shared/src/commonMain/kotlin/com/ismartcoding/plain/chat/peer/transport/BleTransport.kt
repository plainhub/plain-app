package com.ismartcoding.plain.chat.peer.transport

import com.ismartcoding.plain.ble.RustBleWire
import com.ismartcoding.plain.ble.BleDataChannel
import com.ismartcoding.plain.ble.BleServices
import com.ismartcoding.plain.ble.client.BleDeviceApi
import com.ismartcoding.plain.platform.PlatformLock
import com.ismartcoding.plain.platform.bleTransport
import com.ismartcoding.plain.platform.isBleReady
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.*

object BleTransport {
    private val lock = PlatformLock()
    private val mutexes = mutableMapOf<String, Mutex>()

    suspend fun exchange(params: JsonObject): JsonElement {
        check(isBleReady()) { "BLE not ready" }
        val shortId = params.getValue("shortId").jsonPrimitive.content
        val scanner = bleTransport().createScanner()
        val client = scanner.createClient(shortId)
            ?: withTimeoutOrNull(10_000) { scanner.scan(com.ismartcoding.plain.ble.BleUuids.SERVICE_UUID).first { it.id == shortId }; scanner.createClient(shortId) }
            ?: error("BLE device not found")
        val mutex = lock.withLock { mutexes.getOrPut(shortId) { Mutex() } }
        return mutex.withLock {
            val api = BleDeviceApi(client, RustBleWire)
            scanner.pauseScan()
            try {
                BleDataChannel.outgoing(params.getValue("token").jsonPrimitive.content) { message ->
                    check(api.ensureConnected()) { "BLE connect failed" }
                    api.requestAsync(BleServices.http, message)
                }
                JsonPrimitive(true)
            } finally { scanner.resumeScan() }
        }
    }
}
