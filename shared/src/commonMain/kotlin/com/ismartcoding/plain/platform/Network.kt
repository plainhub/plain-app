package com.ismartcoding.plain.platform

enum class NetworkType { WIFI, CELLULAR, ETHERNET, NONE }

/**
 * Current primary network type.
 */
expect fun getNetworkType(): NetworkType

/**
 * Best IPv4 address of the device on the local network, or empty string if none.
 */
expect fun getDeviceIP4(): String

/**
 * Returns the best IP address from [ips] — preferring one on the same subnet
 * as a local interface. Returns empty string if [ips] is empty.
 */
expect fun getBestIp(ips: List<String>): String

expect fun getDeviceIP4sWithPrefixLength(): Set<Pair<String, Short>>

/**
 * Whether a VPN is currently active on the primary network.
 */
expect fun isVPNConnected(): Boolean

fun isSameSubnet(ip1: String, ip2: String, prefixLength: Short): Boolean {
    return try {
        val parts1 = ip1.split(".").map { it.toInt() }
        val parts2 = ip2.split(".").map { it.toInt() }
        if (parts1.size != 4 || parts2.size != 4) return false
        val prefixLen = prefixLength.toInt()
        val mask = if (prefixLen == 0) 0 else (-1 shl (32 - prefixLen))
        val net1 = ((parts1[0] shl 24) or (parts1[1] shl 16) or (parts1[2] shl 8) or parts1[3]) and mask
        val net2 = ((parts2[0] shl 24) or (parts2[1] shl 16) or (parts2[2] shl 8) or parts2[3]) and mask
        net1 == net2
    } catch (e: Exception) {
        false
    }
}
