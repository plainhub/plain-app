package com.ismartcoding.plain.db

import com.ismartcoding.plain.enums.DeviceType
import com.ismartcoding.plain.lib.TimeHelper
import kotlin.time.Instant

/**
 * Persistent history of LAN-discovered devices, so the nearby page renders
 * instantly from cache instead of waiting for a fresh mDNS sweep. Rows are
 * refreshed on every discovery; a row is deleted only once a liveness probe
 * confirms the device left the LAN.
 */
data class DNearbyDeviceCache(
    var id: String,
    var name: String = "",
    /** Comma-joined address list, same encoding as [DPeer.ip]. */
    var ips: String = "",
    var port: Int = 0,
    var deviceType: DeviceType = DeviceType.PHONE,
    var version: String = "",
    var platform: String = "",
    var lastSeen: Instant = TimeHelper.now(),
)
