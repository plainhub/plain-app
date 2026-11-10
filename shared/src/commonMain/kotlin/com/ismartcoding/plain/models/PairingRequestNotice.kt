package com.ismartcoding.plain.models

import com.ismartcoding.plain.platform.system.DeviceType
import kotlinx.serialization.Serializable

@Serializable
data class PairingRequestNotice(
    val fromId: String,
    val toId: String,
    val targetPublicKey: String,
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
    val identityChanged: Boolean,
) {
    fun requestInput() = PairingRequestInput(
        fromId, toId, targetPublicKey, fromName, port, deviceType, ecdhPublicKey, signaturePublicKey,
        timestamp, ips, signature, fromIp, awareSupported,
    )
}
