package com.ismartcoding.plain.discover

import com.ismartcoding.plain.data.DNearbyDevice
import com.ismartcoding.plain.db.DNearbyDeviceCache
import com.ismartcoding.plain.enums.DiscoveryMethod
import com.ismartcoding.plain.lib.TimeHelper
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.platform.AppDatabase

/**
 * Room-backed history of LAN-discovered devices ([DNearbyDeviceCache]): every
 * mDNS sighting is upserted here regardless of page state, so the nearby page
 * can render instantly from history while a fresh sweep runs.
 */
object NearbyDeviceCache {
    suspend fun upsertAsync(device: DNearbyDevice) = withIO {
        AppDatabase.instance.nearbyDeviceCacheDao().upsert(device.toCache())
    }

    suspend fun touchAsync(id: String) = withIO {
        AppDatabase.instance.nearbyDeviceCacheDao().touch(id, TimeHelper.now())
    }

    suspend fun removeAsync(id: String) = withIO {
        AppDatabase.instance.nearbyDeviceCacheDao().delete(id)
    }

    suspend fun getAllAsync(): List<DNearbyDevice> = withIO {
        AppDatabase.instance.nearbyDeviceCacheDao().getAll().map { it.toDevice() }
    }
}

private fun DNearbyDevice.toCache(): DNearbyDeviceCache = DNearbyDeviceCache(
    id = id,
    name = name,
    ips = ips.joinToString(","),
    port = port,
    deviceType = deviceType,
    version = version,
    platform = platform,
    lastSeen = lastSeen,
)

private fun DNearbyDeviceCache.toDevice(): DNearbyDevice = DNearbyDevice(
    id = id,
    name = name,
    ips = ips.split(",").map { it.trim() }.filter { it.isNotEmpty() },
    port = port,
    deviceType = deviceType,
    version = version,
    platform = platform,
    lastSeen = lastSeen,
    discoveryMethods = setOf(DiscoveryMethod.LAN),
)
