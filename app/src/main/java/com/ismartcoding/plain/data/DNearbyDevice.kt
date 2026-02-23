package com.ismartcoding.plain.data

import com.ismartcoding.plain.enums.DeviceType
import kotlin.time.Instant

data class DNearbyDevice(
    val id: String,
    val name: String,
    val ip: String,
    val port: Int,
    val deviceType: DeviceType,
    val version: String,
    val platform: String,
    val lastSeen: Instant
)
