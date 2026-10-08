package com.ismartcoding.plain.features.session

import com.ismartcoding.plain.enums.DeviceType
import kotlinx.serialization.Serializable

@Serializable
data class AuthRequest(
    val password: String,
    val browserName: String,
    val browserVersion: String,
    val osName: String,
    val osVersion: String,
    val isMobile: Boolean,
    val ecdhPublicKey: String = "",
    val peer: AuthPeer? = null,
)
