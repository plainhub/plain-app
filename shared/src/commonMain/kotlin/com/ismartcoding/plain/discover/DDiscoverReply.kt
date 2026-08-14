package com.ismartcoding.plain.discover

import com.ismartcoding.plain.enums.DeviceType
import kotlinx.serialization.Serializable

@Serializable
data class DDiscoverReply(
    val id: String,                // Device ID
    val name: String,              // Device name
    val port: Int,                 // HTTPS API port
    val deviceType: DeviceType,
    val version: String,
    val platform: String,
    val ips: List<String> = emptyList(),
    val awareSupported: Boolean = false,
    val awareRunning: Boolean = false,
)
