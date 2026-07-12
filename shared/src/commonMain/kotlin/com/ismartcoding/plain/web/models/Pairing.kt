package com.ismartcoding.plain.web.models

import com.ismartcoding.plain.data.DNearbyDevice
import com.ismartcoding.plain.data.DPairingRequest
import com.ismartcoding.plain.enums.DeviceType
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class PairingDeviceInput(
    val id: String,
    val name: String,
    val ips: List<String> = emptyList(),
    val port: Int,
    val deviceType: DeviceType,
    val version: String,
    val platform: String,
    val lastSeen: Instant,
) {
    fun toModel(): DNearbyDevice {
        return DNearbyDevice(
            id = id,
            name = name,
            ips = ips,
            port = port,
            deviceType = deviceType,
            version = version,
            platform = platform,
            lastSeen = lastSeen,
        )
    }
}

@Serializable
data class PairingRequestInput(
    val fromId: String,
    val fromName: String,
    val port: Int,
    val deviceType: DeviceType,
    val ecdhPublicKey: String,
    val signaturePublicKey: String,
    val timestamp: Long,
    val ips: List<String> = emptyList(),
    val signature: String = "",
    val fromIp: String = "",
) {
    fun toModel(): DPairingRequest {
        return DPairingRequest(
            fromId = fromId,
            fromName = fromName,
            port = port,
            deviceType = deviceType,
            ecdhPublicKey = ecdhPublicKey,
            signaturePublicKey = signaturePublicKey,
            timestamp = timestamp,
            ips = ips,
            signature = signature,
            fromIp = fromIp,
        )
    }
}
