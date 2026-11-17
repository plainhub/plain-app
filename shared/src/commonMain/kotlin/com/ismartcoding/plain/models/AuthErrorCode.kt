package com.ismartcoding.plain.models

import kotlinx.serialization.Serializable

@Serializable
enum class AuthErrorCode {
    CANCELED,
    INTERNAL_ERROR,
    INVALID_LOGIN_REQUEST,
    INVALID_PASSWORD,
    PASSWORD_SETUP_REQUIRED,
    PEER_IDENTITY_CHANGED,
    REJECTED,
    REMOTE_ACCESS_DISABLED,
    TOO_MANY_LOGIN_ATTEMPTS,
    TIMEOUT;

    val wire: String get() = name

    companion object {
        fun fromWire(value: String?): AuthErrorCode? = entries.firstOrNull { it.name == value }
    }
}
