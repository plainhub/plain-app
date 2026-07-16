package com.ismartcoding.plain.platform

actual fun getNetworkType(): NetworkType = NetworkType.NONE

actual fun getDeviceIP4(): String = ""

actual fun getBestIp(ips: List<String>): String = ips.firstOrNull() ?: ""

actual fun getDeviceIP4sWithPrefixLength(): Set<Pair<String, Short>> {
    return emptySet()
}

actual fun isVPNConnected(): Boolean = false

actual fun isPortInUse(port: Int): Boolean = false