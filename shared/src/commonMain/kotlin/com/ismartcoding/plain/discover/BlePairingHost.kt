package com.ismartcoding.plain.discover

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.*

object BlePairingHost {
    private class Handle(val job: Job?) { var connection: BlePairConnection? = null }
    private val mutex = Mutex()
    private val handles = mutableMapOf<String, Handle>()
    private val closed = linkedSetOf<String>()

    suspend fun handle(method: String, params: JsonObject): JsonElement {
        val generation = params.getValue("generation").jsonPrimitive.content
        return when (method) {
            "blePairConnect" -> {
                val operation = currentCoroutineContext()[Job]
                val entry = mutex.withLock {
                    if (generation in closed || handles.containsKey(generation) || handles.size >= 2) return JsonPrimitive(false)
                    Handle(operation).also { handles[generation] = it }
                }
                val device = RustNearbyDevices.client(params.getValue("id").jsonPrimitive.content) ?: return JsonPrimitive(false)
                val connection = BlePairConnection(device)
                val installed = mutex.withLock {
                    if (handles[generation] !== entry) false else { entry.connection = connection; true }
                }
                if (!installed) { connection.close(); return JsonPrimitive(false) }
                JsonPrimitive(connection.connect())
            }
            "blePairSend" -> JsonPrimitive(connection(generation).send(params.getValue("body").jsonPrimitive.content))
            "blePairWait" -> connection(generation).wait(params.getValue("timeoutMs").jsonPrimitive.long.coerceIn(1, 1_000))
            "blePairClose" -> {
                val entry = mutex.withLock {
                    closed.add(generation)
                    while (closed.size > 128) closed.remove(closed.first())
                    handles.remove(generation)
                }
                entry?.job?.cancel()
                entry?.connection?.close()
                JsonPrimitive(true)
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
            try { it.connection?.close() } catch (error: Exception) { com.ismartcoding.plain.lib.logcat.LogCat.e("BLE handle cleanup", error) }
        }
    }
}
