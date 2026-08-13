package com.ismartcoding.plain.httpserver

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
)

@Serializable
data class AuthResponse(
    val clientId: String,
    val status: AuthStatus,
    val ecdhPublicKey: String = "",
    val signature: String = "",
    val timestamp: Long = 0L,
) {
    fun toSignatureData(): String =
        "$clientId|${status.name}|$ecdhPublicKey|$timestamp"
}

enum class AuthStatus {
    PENDING,
    COMPLETED,
}
