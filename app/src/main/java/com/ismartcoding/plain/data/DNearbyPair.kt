package com.ismartcoding.plain.data

import com.ismartcoding.plain.enums.DeviceType
import com.ismartcoding.plain.helpers.TimeHelper
import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class DPairingRequest(
    val fromId: String,
    val fromName: String,
    val port: Int,
    val deviceType: DeviceType,
    val ecdhPublicKey: String, // ECDH public key for encrypted communication
    val signaturePublicKey: String, // Raw Ed25519 signature public key (32 bytes, Base64 encoded)
    val timestamp: Long, // Timestamp for replay attack prevention
    val ips: List<String> = emptyList(), // All IP addresses of the requesting device
    var signature: String = "" // Ed25519 signature of request content (Base64 encoded)
) {
    fun toSignatureData(): String {
        return "$fromId|$fromName|$port|${deviceType.value}|$ecdhPublicKey|$signaturePublicKey|$timestamp|${ips.joinToString(",")}"
    }
}

@Serializable
data class DPairingResponse(
    val fromId: String,
    val toId: String,
    val port: Int,
    val deviceType: DeviceType,
    val ecdhPublicKey: String, // ECDH public key for encrypted communication
    val signaturePublicKey: String, // Raw Ed25519 signature public key (32 bytes, Base64 encoded)
    val accepted: Boolean,
    val timestamp: Long, // Timestamp for replay attack prevention
    val ips: List<String> = emptyList(), // All IP addresses of the responding device
    var signature: String  = ""// Ed25519 signature of response content (Base64 encoded)
) {
    fun toSignatureData(): String {
        return "$fromId|$toId|$port|${deviceType.value}|$ecdhPublicKey|$signaturePublicKey|$accepted|$timestamp|${ips.joinToString(",")}"
    }
}

@Serializable
data class DPairingCancel(
    val fromId: String,
    val toId: String,
)

data class DPairingSession(
    val deviceId: String,
    val deviceName: String,
    val deviceIp: String,
    val keyPair: java.security.KeyPair,
    val timestamp: Instant = TimeHelper.now()
)