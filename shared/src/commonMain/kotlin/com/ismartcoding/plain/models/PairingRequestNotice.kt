package com.ismartcoding.plain.models

import com.ismartcoding.plain.platform.system.DeviceType
import kotlinx.serialization.Serializable

@Serializable
data class PairingRequestNotice(
    val fromId: String,
    val fromName: String,
    val port: Int,
    val deviceType: DeviceType,
    val ecdhPublicKey: String,
    val signaturePublicKey: String,
    val timestamp: Long,
    val ips: List<String>,
    val signature: String,
    val fromIp: String,
    val awareSupported: Boolean,
    val verificationCode: String,
) {
    fun requestInput() = PairingRequestInput(
        fromId, fromName, port, deviceType, ecdhPublicKey, signaturePublicKey,
        timestamp, ips, signature, fromIp, awareSupported,
    )
}
