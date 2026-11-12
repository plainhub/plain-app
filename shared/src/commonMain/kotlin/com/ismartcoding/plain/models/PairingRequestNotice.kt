package com.ismartcoding.plain.models

import com.ismartcoding.plain.platform.system.DeviceType
import kotlinx.serialization.Serializable

@Serializable
data class PairingRequestNotice(
    val fromId: String,
    val fromName: String,
    val fromIp: String,
    val deviceType: DeviceType,
    val verificationCode: String,
    val identityChanged: Boolean,
)
