package com.ismartcoding.plain.discover

import com.ismartcoding.plain.chat.callChatStore
import com.ismartcoding.plain.data.DNearbyDevice
import com.ismartcoding.plain.enums.DeviceType
import com.ismartcoding.plain.enums.DiscoveryMethod
import kotlinx.serialization.json.*
import kotlin.time.Instant

object NearbyDeviceCache {
    suspend fun upsertAsync(device: DNearbyDevice) {
        callChatStore("saveNearby") { put("item", buildJsonObject {
            put("id", device.id); put("name", device.name); put("ips", JsonArray(device.ips.map(::JsonPrimitive))); put("port", device.port)
            put("device_type", device.deviceType.name); put("version", device.version); put("platform", device.platform); put("last_seen", device.lastSeen.toString())
        }) }
    }
    suspend fun touchAsync(id: String) { callChatStore("touchNearby") { put("id", id) } }
    suspend fun removeAsync(id: String) { callChatStore("deleteNearby") { put("id", id) } }
    suspend fun getAllAsync(): List<DNearbyDevice> = callChatStore("nearby").jsonArray.map { value -> value.jsonObject.let { row ->
        fun string(key: String) = row.getValue(key).jsonPrimitive.content
        DNearbyDevice(id = string("id"), name = string("name"), ips = row.getValue("ips").jsonArray.map { it.jsonPrimitive.content }, port = row.getValue("port").jsonPrimitive.int, deviceType = DeviceType.valueOf(string("device_type")), version = string("version"), platform = string("platform"), lastSeen = Instant.parse(string("last_seen")), discoveryMethods = setOf(DiscoveryMethod.LAN), bestIp = string("bestIp"))
    } }
}
