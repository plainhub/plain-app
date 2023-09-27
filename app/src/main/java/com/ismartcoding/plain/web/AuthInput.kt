package com.ismartcoding.plain.web

import kotlinx.serialization.Serializable

@Serializable
data class AuthRequest(
    val password: String,
    val browserName: String,
    val browserVersion: String,
    val osName: String,
    val osVersion: String,
    val isMobile: Boolean,
)

@Serializable
data class AuthResponse(val status: AuthStatus, val token: String = "")

enum class AuthStatus {
    PENDING,
    COMPLETED,
}
