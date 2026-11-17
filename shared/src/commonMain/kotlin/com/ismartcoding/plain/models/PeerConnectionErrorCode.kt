package com.ismartcoding.plain.models

import kotlinx.serialization.Serializable

@Serializable
enum class PeerConnectionErrorCode {
    CONNECTION_TIMEOUT,
    INVALID_SESSION,
    MANAGED_SOCKET_CLOSED,
    MANAGED_SUBSCRIPTION_CLOSED,
    PEER_REMOVED,
    PROXY_AUTH_FAILED,
    PEER_HTTPS_UNAVAILABLE,
    PEER_ADDRESS_UNAVAILABLE;

    val wire: String get() = name

    companion object {
        fun fromWire(value: String?): PeerConnectionErrorCode? = entries.firstOrNull { it.name == value }
    }
}
