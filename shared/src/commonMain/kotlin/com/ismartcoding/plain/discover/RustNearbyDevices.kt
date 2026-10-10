package com.ismartcoding.plain.discover

import com.ismartcoding.plain.api.RustContentApi
import com.ismartcoding.plain.ble.client.BleGattClient
import com.ismartcoding.plain.data.DNearbyDevice
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.ui.models.NearbyViewModel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.*

object RustNearbyDevices {
    private val mutex = Mutex()
    private val clients = mutableMapOf<String, BleGattClient>()
    private var revision = -1L

    internal suspend fun client(id: String): BleGattClient? = mutex.withLock { clients[id] }

    suspend fun scanning() = mutex.withLock {
        apply(call(buildJsonObject {
            put("action", "bleScanning"); put("ble", NearbyViewModel.isBleScanning.value)
        }))
    }

    suspend fun seen(device: DNearbyDevice, visible: Boolean = true, resident: Boolean = false) = mutex.withLock {
        device.bleClient?.let { clients[device.id] = it }
        val snapshot = call(buildJsonObject {
            put("action", "seen"); put("device", Json.parseToJsonElement(JsonHelper.jsonEncode(device))); put("visible", visible); put("resident", resident)
        })
        if (snapshot["ignored"]?.jsonPrimitive?.booleanOrNull == true) return@withLock false
        apply(snapshot)
        true
    }

    suspend fun refresh() = mutex.withLock {
        apply(call(buildJsonObject { put("action", "snapshot") }))
    }

    private suspend fun call(body: JsonObject) = RustContentApi.postJsonOrThrow("chat/nearby-devices", body).getValue("result").jsonObject

    private fun apply(snapshot: JsonObject) {
        val next = snapshot["revision"]?.jsonPrimitive?.long ?: return
        if (next < revision) return
        revision = next
        val devices = snapshot.getValue("devices").jsonArray.map {
            val device = JsonHelper.jsonDecode<DNearbyDevice>(it.toString())
            device.copy(bestIp = it.jsonObject.getValue("bestIp").jsonPrimitive.content, bleClient = clients[device.id], status = NearbyViewModel.getStatus(device.id, device.status == com.ismartcoding.plain.ui.models.NearbyItemStatus.PAIRED))
        }
        clients.keys.retainAll(devices.map { it.id }.toSet())
        NearbyViewModel.nearbyDevices.value = devices
    }
}
