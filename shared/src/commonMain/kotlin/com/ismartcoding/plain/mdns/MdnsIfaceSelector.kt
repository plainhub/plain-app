package com.ismartcoding.plain.mdns

/**
 * Collects candidate LAN (non-loopback, non-mobile-data) interfaces with their
 * primary IPv4 address string. Platform-specific: java.net on Android, getifaddrs on iOS.
 */
internal expect fun candidateInterfaces(): List<Pair<MdnsIface, String>>

/** Returns true for mobile-data-only bearer interface names (never LAN). */
internal fun isMobileDataInterface(name: String): Boolean =
    name.startsWith("rmnet") || name.startsWith("ccmni") ||
        name.startsWith("v4-rmnet") || name.startsWith("v6-rmnet") ||
        name.startsWith("clat") || name.startsWith("v4-ccmni")

/** Finds the interface whose subnet contains [senderIp]; falls back to first candidate. */
internal fun findResponseIface(
    senderIp: String,
    candidates: List<Pair<MdnsIface, String>>,
): Pair<MdnsIface, String> {
    for ((iface, localIp) in candidates) {
        val bits = iface.networkPrefixLength.toInt()
        val mask = if (bits == 0) 0 else (0xFFFFFFFFL shl (32 - bits)).toInt()
        if ((ipToInt(localIp) and mask) == (ipToInt(senderIp) and mask)) return iface to localIp
    }
    return candidates.first()
}

/** Parses an IPv4 string to a 32-bit big-endian integer for subnet arithmetic. */
internal fun ipToInt(ip: String): Int {
    val p = ip.split(".")
    if (p.size != 4) return 0
    return ((p[0].toIntOrNull() ?: 0) shl 24) or
        ((p[1].toIntOrNull() ?: 0) shl 16) or
        ((p[2].toIntOrNull() ?: 0) shl 8) or
        (p[3].toIntOrNull() ?: 0)
}

/** Parses an IPv4 string to a 4-byte array for DNS A records. */
internal fun ipToBytes(ip: String): ByteArray {
    val p = ip.split(".")
    return byteArrayOf(
        (p.getOrNull(0)?.toIntOrNull() ?: 0).toByte(),
        (p.getOrNull(1)?.toIntOrNull() ?: 0).toByte(),
        (p.getOrNull(2)?.toIntOrNull() ?: 0).toByte(),
        (p.getOrNull(3)?.toIntOrNull() ?: 0).toByte(),
    )
}
