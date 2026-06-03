package com.ismartcoding.plain.web.models

import com.ismartcoding.plain.data.DPairingRequest
import com.ismartcoding.plain.enums.DeviceType
import kotlinx.serialization.Serializable

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
