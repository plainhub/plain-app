package com.ismartcoding.plain.models

import kotlinx.serialization.Serializable

@Serializable
enum class PairingErrorCode {
    CANCELED,
    TIMEOUT,
    INTERNAL_ERROR,
    REJECTED,
    BLE_CONNECTION_CLOSING,
    BLE_PAIRING_CAPACITY_EXCEEDED,
    FAILED_CONNECT,
    PAIRING_TRANSPORT_UNAVAILABLE,
    PAIRING_ADDRESS_UNAVAILABLE,
    FAILED_CONNECT_WS,
    INVALID_AUTH_RESPONSE,
    INVALID_SESSION,
    PASSWORD_REQUIRED,
    PEER_IDENTITY_CHANGED,
    REMOTE_ACCESS_DISABLED,
    SIGNATURE_VERIFICATION_FAILED,
    CHAT_PAIRING_UNAVAILABLE,
    INVALID_PASSWORD,
    LOGIN_EXPIRED,
    TOO_MANY_LOGIN_ATTEMPTS,
    PASSWORD_SETUP_REQUIRED,
    SECURE_LOGIN_UNAVAILABLE,
    INVALID_LOGIN_REQUEST,
    TOO_MANY_LOGIN_CONNECTIONS;

    val wire: String get() = name

    companion object {
        fun fromWire(value: String?): PairingErrorCode? = entries.firstOrNull { it.name == value }
    }
}
