package com.ismartcoding.plain.mdns

import com.ismartcoding.plain.lib.helpers.NetworkHelper
import java.net.Inet4Address
import java.net.NetworkInterface

internal fun candidateInterfaces(): List<Pair<NetworkInterface, Inet4Address>> {
    return runCatching {
        NetworkInterface.getNetworkInterfaces()
            ?.asSequence()
            ?.filter { it.isUp }
            ?.filterNot { it.isLoopback }
            ?.filterNot { isMobileDataInterface(it.name) }
            ?.mapNotNull { iface ->
                val ip = iface.inetAddresses
                    .asSequence()
                    .filterIsInstance<Inet4Address>()
                    .firstOrNull { !it.isLoopbackAddress }
                ip?.let { iface to it }
            }
            ?.toList()
            ?: emptyList()
    }.getOrElse { emptyList() }
}

/** Returns true for mobile-data-only bearer interface names (never LAN). */
internal fun isMobileDataInterface(name: String): Boolean =
    name.startsWith("rmnet") || name.startsWith("ccmni") ||
        name.startsWith("v4-rmnet") || name.startsWith("v6-rmnet") ||
        name.startsWith("clat") || name.startsWith("v4-ccmni")

internal fun findResponseIface(
    senderIp: Inet4Address,
    candidates: List<Pair<NetworkInterface, Inet4Address>>,
): Pair<NetworkInterface, Inet4Address> {
    for ((iface, localIp) in candidates) {
        val ia = iface.interfaceAddresses.firstOrNull { it.address == localIp } ?: continue
        val bits = ia.networkPrefixLength.toInt()
        val mask = if (bits == 0) 0 else (0xFFFFFFFFL shl (32 - bits)).toInt()
        if ((ipToInt(localIp) and mask) == (ipToInt(senderIp) and mask)) return iface to localIp
    }
    return candidates.first()
}

/** Converts an [Inet4Address] to a 32-bit big-endian integer for subnet arithmetic. */
internal fun ipToInt(ip: Inet4Address): Int {
    val b = ip.address
    return ((b[0].toInt() and 0xFF) shl 24) or ((b[1].toInt() and 0xFF) shl 16) or
        ((b[2].toInt() and 0xFF) shl 8) or (b[3].toInt() and 0xFF)
}
