package com.ismartcoding.plain.rust.host.network

import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.platform.network.ble.BlePairConnection
import com.ismartcoding.plain.platform.network.bleTransport
import com.ismartcoding.plain.rust.host.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import kotlin.io.encoding.Base64

object BlePairingHost {
    private class Handle(val job: Job?) { var connection: BlePairConnection? = null }
    private val mutex = Mutex()
    private val handles = mutableMapOf<String, Handle>()
    private val closed = linkedSetOf<String>()

    internal suspend fun handle(method: String, params: JsonObject): HostResult {
        return when (method) {
            "blePairConnect" -> {
                val request = JsonHelper.jsonDecodeFromElement<BleConnectRequest>(params)
                val generation = request.generation
                val operation = currentCoroutineContext()[Job]
                val entry = mutex.withLock {
                    if (generation in closed || handles.containsKey(generation) || handles.size >= 2) return hostResult(false)
                    Handle(operation).also { handles[generation] = it }
                }
                val device = bleTransport().createScanner()
                    .createClient(request.clientId) ?: return hostResult(false)
                val connection = BlePairConnection(device)
                val installed = mutex.withLock {
                    if (handles[generation] !== entry) false else { entry.connection = connection; true }
                }
                if (!installed) { connection.close(); return hostResult(false) }
                hostResult(connection.connect())
            }
            "blePairSend" -> {
                val request = JsonHelper.jsonDecodeFromElement<BleSendRequest>(params)
                hostResult(connection(request.generation).send(Base64.decode(request.packet)))
            }
            "blePairWait" -> {
                val request = JsonHelper.jsonDecodeFromElement<BleWaitRequest>(params)
                hostResult(connection(request.generation).wait(request.timeoutMs.coerceIn(1, 1_000)))
            }
            "blePairClose" -> {
                val generation = JsonHelper.jsonDecodeFromElement<BleGenerationRequest>(params).generation
                val entry = mutex.withLock {
                    closed.add(generation)
                    while (closed.size > 128) closed.remove(closed.first())
                    handles.remove(generation)
                }
                entry?.job?.cancel()
                entry?.connection?.close()
                hostResult(true)
            }
            else -> error("Unsupported BLE operation")
        }
    }

    private suspend fun connection(generation: String): BlePairConnection = mutex.withLock {
        checkNotNull(handles[generation]?.connection) { "BLE handle unavailable" }
    }

    suspend fun disconnect() {
        val active = mutex.withLock { handles.values.toList().also { handles.clear(); closed.clear() } }
        active.forEach {
            it.job?.cancel()
            try { it.connection?.close() } catch (error: Exception) { LogCat.e("BLE handle cleanup", error) }
        }
    }
}

@Serializable
private data class BleGenerationRequest(val generation: String)

@Serializable
private data class BleConnectRequest(val generation: String, val clientId: String)

@Serializable
private data class BleSendRequest(val generation: String, val packet: String)

@Serializable
private data class BleWaitRequest(val generation: String, val timeoutMs: Long)
