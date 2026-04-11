package com.ismartcoding.plain.web

import com.ismartcoding.lib.helpers.NetworkHelper
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * Interface selection utilities for the mDNS responder.
 * Extracted to keep MdnsHostResponder under the line limit and to enable unit testing
 * of the purely logical (no-Context) functions.
 */

/**
 * Returns non-loopback, non-VPN, multicast-capable interfaces that carry an IPv4
 * address. Mobile-data bearers (rmnet*, ccmni*) are excluded — they are never part
 * of a LAN and would cause mDNS replies to egress via the wrong path.
 */
internal fun candidateInterfaces(): List<Pair<NetworkInterface, Inet4Address>> {
    val result = mutableListOf<Pair<NetworkInterface, Inet4Address>>()
    runCatching {
        val ifaces = NetworkInterface.getNetworkInterfaces() ?: return result
        for (iface in ifaces.asSequence()) {
            if (!iface.isUp || iface.isLoopback || !iface.supportsMulticast()) continue
            if (NetworkHelper.isVpnInterface(iface.name)) continue
            if (isMobileDataInterface(iface.name)) continue
            val ip = iface.inetAddresses.asSequence()
                .filterIsInstance<Inet4Address>()
                .firstOrNull { !it.isLoopbackAddress } ?: continue
            result += iface to ip
        }
    }
    return result
}

/** Returns true for mobile-data-only bearer interface names (never LAN). */
internal fun isMobileDataInterface(name: String): Boolean =
    name.startsWith("rmnet") || name.startsWith("ccmni")

/**
 * Returns the local interface whose subnet contains [senderIp], or the first candidate
 * as a fallback. Setting `socket.networkInterface` to the returned interface before
 * each send ensures the reply egresses via the physical path the query arrived on
 * (fixes "reply goes out via mobile-data rmnet instead of wlan/ap0" bug).
 */
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
