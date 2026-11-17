package com.ismartcoding.plain.models

import kotlinx.serialization.Serializable

@Serializable
enum class AuthErrorCode {
    CANCELED,
    EVENT_STORAGE_FAILED,
    INTERNAL_ERROR,
    INVALID_AUTH_RESPONSE,
    INVALID_IDENTITY,
    INVALID_LOGIN_IDENTITY,
    INVALID_LOGIN_KEY,
    INVALID_LOGIN_REQUEST,
    INVALID_PASSWORD,
    LOGIN_EXPIRED,
    PASSWORD_SETUP_REQUIRED,
    PEER_IDENTITY_CHANGED,
    REJECTED,
    REMOTE_ACCESS_DISABLED,
    TOO_MANY_LOGIN_ATTEMPTS,
    TOO_MANY_LOGIN_CONNECTIONS,
    TIMEOUT,
    SECURE_LOGIN_UNAVAILABLE,
    SIGNATURE_VERIFICATION_FAILED,
    FAILED_CONNECT_WS,
    PROXY_AUTH_FAILED,
    FAILED,
    UNAUTHORIZED,
    CHAT_PAIRING_UNAVAILABLE;

    val wire: String get() = name

    companion object {
        fun fromWire(value: String?): AuthErrorCode? = entries.firstOrNull { it.name == value }
    }
}
