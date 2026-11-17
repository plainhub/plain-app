package com.ismartcoding.plain.models

import kotlinx.serialization.Serializable

@Serializable
enum class PairingErrorCode {
    TIMEOUT,
    REJECTED,
    PASSWORD_REQUIRED,
    INVALID_PASSWORD,
    PEER_IDENTITY_CHANGED,
    TOO_MANY_LOGIN_ATTEMPTS,
    INTERNAL_ERROR;

    val wire: String get() = name

    companion object {
        fun fromWire(value: String?): PairingErrorCode? = entries.firstOrNull { it.name == value }
    }
}
