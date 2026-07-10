package com.ismartcoding.plain.data

import com.ismartcoding.plain.ble.client.BleGattClient
import com.ismartcoding.plain.enums.DeviceType
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.time.Instant

@Serializable
data class DNearbyDevice(
    val id: String,
    val name: String,
    val ips: List<String> = emptyList(),
    val port: Int,
    val deviceType: DeviceType,
    val version: String,
    val platform: String,
    val lastSeen: Instant,
    val discoveredViaLan: Boolean = true,
    val discoveredViaBle: Boolean = false,
    @Transient
    val bleClient: BleGattClient? = null,
)