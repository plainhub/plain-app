package com.ismartcoding.plain.features.sessions

import com.ismartcoding.plain.platform.system.DeviceType
import kotlinx.serialization.Serializable

@Serializable
data class AuthRequest(
    val password: String = "",
    val appName: String,
    val appVersion: String,
    val osName: String,
    val osVersion: String,
    val ecdhPublicKey: String = "",
    val peer: AuthPeer? = null,
    val verificationCode: String = "",
)
