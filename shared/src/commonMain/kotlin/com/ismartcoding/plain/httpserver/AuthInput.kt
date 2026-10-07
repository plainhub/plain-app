package com.ismartcoding.plain.httpserver

import com.ismartcoding.plain.enums.DeviceType
import kotlinx.serialization.Serializable

@Serializable
data class AuthPeer(
    val deviceName: String,
    val port: Int,
    val deviceType: DeviceType,
    val ips: List<String>,
    val signaturePublicKey: String,
)

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

@Serializable
data class AuthResponse(
    val clientId: String,
    val status: AuthStatus,
    val ecdhPublicKey: String = "",
    val signature: String = "",
    val timestamp: Long = 0L,
    val chatPaired: Boolean = false,
)

enum class AuthStatus {
    PENDING,
    COMPLETED,
}
