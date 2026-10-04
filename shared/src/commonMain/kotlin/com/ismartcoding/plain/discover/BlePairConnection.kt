package com.ismartcoding.plain.discover

import com.ismartcoding.plain.api.clientHeadersMap
import com.ismartcoding.plain.ble.BleRequestData
import com.ismartcoding.plain.ble.BleServices
import com.ismartcoding.plain.ble.client.BleDeviceApi
import com.ismartcoding.plain.ble.client.BleGattClient
import com.ismartcoding.plain.platform.bleTransport
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.*

internal class BlePairConnection(private val device: BleGattClient) {
    private val scanner = bleTransport().createScanner()
    private val api = BleDeviceApi(device)
    private val mutex = Mutex()
    private var job: Job? = null
    private var paused = false
    private var notifications = false
    private var closed = false

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
        return api.sendRequest(BleServices.nearby, BleRequestData.create(clientHeadersMap()).copy(body = body))
    }

    suspend fun wait(timeoutMs: Long): JsonObject {
        mutex.withLock {
            check(!closed) { "BLE handle closed" }
            job = currentCoroutineContext()[Job]
        }
        val notification = device.waitForNotification(BleServices.nearby, timeoutMs)
        return buildJsonObject {
            put("connected", api.isConnected())
            put("notification", notification?.let(::JsonPrimitive) ?: JsonNull)
        }
    }

    suspend fun close() {
        val state = mutex.withLock {
            if (closed) return
            closed = true
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
