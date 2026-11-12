package com.ismartcoding.plain.models

import com.ismartcoding.plain.features.nearby.DiscoveryMethod
import com.ismartcoding.plain.features.nearby.PeerStatus
import com.ismartcoding.plain.platform.system.DeviceType
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class NearbyDevice(
    val id: String,
    val name: String,
    val signaturePublicKey: String,
    val ips: List<String> = emptyList(),
    val port: Int,
    val remoteAccess: Boolean? = null,
    val deviceType: DeviceType,
    val version: String,
    val platform: String,
    val lastSeen: Instant,
    val discoveryMethods: List<DiscoveryMethod> = emptyList(),
    val bestIp: String = "",
    val status: PeerStatus,
)
