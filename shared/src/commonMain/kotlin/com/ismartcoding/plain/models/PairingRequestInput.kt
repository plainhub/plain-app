package com.ismartcoding.plain.models

import com.ismartcoding.plain.platform.system.DeviceType
import kotlinx.serialization.Serializable

@Serializable
data class PairingRequestInput(
    var fromId: String,
    var toId: String,
    var targetPublicKey: String,
    var fromName: String,
    var port: Int,
    var deviceType: DeviceType,
    var ecdhPublicKey: String,
    var signaturePublicKey: String,
    var timestamp: Long,
    var ips: List<String>,
    var signature: String,
    var fromIp: String,
    var awareSupported: Boolean,
)
