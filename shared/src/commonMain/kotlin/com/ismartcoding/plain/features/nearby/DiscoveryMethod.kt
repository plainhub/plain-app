package com.ismartcoding.plain.features.nearby

import kotlinx.serialization.Serializable

@Serializable
enum class DiscoveryMethod {
    LAN,
    DNS_SD,
    BLE,
    QR,
}
