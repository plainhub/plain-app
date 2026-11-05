package com.ismartcoding.plain.platform.network.ble

import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.platform.network.ble.client.BleDeviceApi
import com.ismartcoding.plain.helpers.Base64Lenient
import com.ismartcoding.plain.platform.system.PlatformLock
import com.ismartcoding.plain.platform.network.bleTransport
import com.ismartcoding.plain.platform.network.isBleReady
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

object PeerBleExchange {
    @Serializable
    private data class Reply(val response: String)

    private val lock = PlatformLock()
    private val mutexes = mutableMapOf<String, Mutex>()

    suspend fun exchange(params: JsonObject): JsonElement {
        check(isBleReady()) { "BLE not ready" }
        val input = JsonHelper.jsonDecodeFromElement<ExchangeRequest>(params)
        val clientId = input.clientId
        val request = Base64Lenient.decode(input.request)
        val scanner = bleTransport().createScanner()
        val client = scanner.createClient(clientId)
            ?: withTimeoutOrNull(10_000) { scanner.scan(clientId).first { it.id == clientId }; scanner.createClient(clientId) }
            ?: error("BLE device not found")
        val mutex = lock.withLock { mutexes.getOrPut(clientId) { Mutex() } }
        return mutex.withLock {
            val api = BleDeviceApi(client, RustBleWire)
            scanner.pauseScan()
            try {
                check(api.ensureConnected()) { "BLE connect failed" }
                val response = api.requestAsync(BleServices.http, request)
                JsonHelper.jsonEncodeToElement(Reply(Base64Lenient.encode(response)))
            } finally { scanner.resumeScan() }
        }
    }
}

@Serializable
private data class ExchangeRequest(val clientId: String, val request: String)
