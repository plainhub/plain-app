package com.ismartcoding.plain.discover

import com.ismartcoding.plain.api.clientHeadersMap
import com.ismartcoding.plain.ble.RustBleWire
import com.ismartcoding.plain.ble.BleServices
import com.ismartcoding.plain.ble.client.BleDeviceApi
import com.ismartcoding.plain.ble.client.BleGattClient
import com.ismartcoding.plain.platform.bleTransport
import com.ismartcoding.plain.lib.JsonHelper
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.*
import kotlinx.serialization.Serializable

internal class BlePairConnection(private val device: BleGattClient) {
    private val scanner = bleTransport().createScanner()
    private val api = BleDeviceApi(device, RustBleWire)
    private val mutex = Mutex()
    private var job: Job? = null
    private var paused = false
    private var notifications = false
    private var closed = false
    private val assembly = RustBleWire.assembler()
    private var requestId = 0

    suspend fun connect(): Boolean {
        mutex.withLock {
            check(!closed) { "BLE handle closed" }
            job = currentCoroutineContext()[Job]
            scanner.pauseScan()
            paused = true
        }
        return api.ensureConnected() && api.isConnected()
    }

    suspend fun send(body: String): Boolean {
        mutex.withLock {
            check(!closed) { "BLE handle closed" }
            job = currentCoroutineContext()[Job]
            notifications = true
        }
        requestId = RustBleWire.nextRequestId()
        return api.sendRequest(BleServices.nearby, RustBleWire.nearby(body.encodeToByteArray()), requestId)
    }

    suspend fun wait(timeoutMs: Long): JsonObject {
        mutex.withLock {
            check(!closed) { "BLE handle closed" }
            job = currentCoroutineContext()[Job]
        }
        val notification = withTimeoutOrNull(timeoutMs) {
            var body: String? = null
            while (body == null) {
                val frame = device.waitForNotification(BleServices.nearby, timeoutMs) ?: break
                val message = assembly.push(frame)
                check(assembly.response && assembly.requestId == requestId) { "Unexpected Nearby response" }
                if (message != null) body = RustBleWire.nearbyBody(message).decodeToString(throwOnInvalidSequence = true)
            }
            body
        }
        return JsonHelper.jsonEncodeToElement(PollFacts(api.isConnected(), notification)).jsonObject
    }

    @Serializable private data class PollFacts(val connected: Boolean, val notification: String?)

    suspend fun close() {
        val state = mutex.withLock {
            if (closed) return
            closed = true
            assembly.close()
            Triple(job, paused, notifications)
        }
        if (state.first != currentCoroutineContext()[Job]) state.first?.cancel()
        withContext(NonCancellable) {
            try {
                if (state.first != currentCoroutineContext()[Job]) withTimeout(5_000) { state.first?.cancelAndJoin() }
                if (state.third) withTimeout(5_000) { device.setNotification(BleServices.nearby, false) }
            } catch (_: Exception) {
            } finally {
                if (state.second) {
                    try { scanner.resumeScan() } finally { scanner.teardownConnection(device) }
                }
            }
        }
    }
}
