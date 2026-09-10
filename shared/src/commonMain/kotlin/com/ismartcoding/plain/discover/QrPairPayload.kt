package com.ismartcoding.plain.discover

import com.ismartcoding.plain.data.DNearbyDevice
import com.ismartcoding.plain.enums.DeviceType
import com.ismartcoding.plain.enums.DiscoveryMethod
import com.ismartcoding.plain.lib.TimeHelper
import com.ismartcoding.plain.lib.extensions.urlDecode

/**
 * The `plainapp://pair?v=1&id=…&name=…&ips=…&port=…` code that plain-desktop
 * shows as a QR fallback when mDNS/BLE discovery is unavailable (TUN proxies,
 * AP isolation, firewalls). Scanning it lets this device initiate pairing
 * over LAN `POST /nearby` directly, bypassing discovery entirely. Identity is
 * still authenticated by the signed pairing handshake — this payload only
 * bootstraps the connection.
 */
data class QrPairPayload(
    val id: String,
    val name: String,
    val ips: List<String>,
    val port: Int,
) {
    fun toDevice(): DNearbyDevice =
        DNearbyDevice(
            id = id,
            name = name,
            ips = ips,
            port = port,
            deviceType = DeviceType.COMPUTER,
            version = "",
            platform = "",
            lastSeen = TimeHelper.now(),
            discoveryMethods = setOf(DiscoveryMethod.QR),
        )

    companion object {
        const val SCHEME = "plainapp://pair?"

        /** Returns null for any text that is not a plainapp pairing code. */
        fun parse(text: String): QrPairPayload? {
            if (!text.startsWith(SCHEME)) return null
            var id = ""
            var name = ""
            val ips = mutableListOf<String>()
            var port = 0
            for (param in text.removePrefix(SCHEME).split('&')) {
                val idx = param.indexOf('=')
                if (idx <= 0) continue
                val value = param.substring(idx + 1).urlDecode()
                when (param.substring(0, idx)) {
                    "id" -> id = value
                    "name" -> name = value
                    "ips" -> ips.addAll(value.split(',').filter { it.isNotBlank() })
                    "port" -> port = value.toIntOrNull() ?: 0
                }
            }
            if (id.isBlank() || name.isBlank() || ips.isEmpty() || port <= 0) return null
            return QrPairPayload(id, name, ips, port)
        }
    }
}
