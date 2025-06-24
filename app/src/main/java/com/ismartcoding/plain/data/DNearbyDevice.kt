package com.ismartcoding.plain.data

import com.ismartcoding.plain.enums.DeviceType
import kotlinx.datetime.Instant

data class DNearbyDevice(
    val id: String,
    val name: String,
    val ip: String,
    val deviceType: DeviceType,
    val version: String,
    val platform: String,
    val lastSeen: Instant
)
