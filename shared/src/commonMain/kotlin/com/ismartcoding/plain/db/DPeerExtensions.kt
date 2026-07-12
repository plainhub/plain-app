package com.ismartcoding.plain.db

import com.ismartcoding.plain.platform.getDeviceIP4sWithPrefixLength
import com.ismartcoding.plain.platform.isSameSubnet
import com.ismartcoding.plain.lib.extensions.urlEncode

fun DPeer.getBestIp(): String {
    val ips = getIpList()
    if (ips.isEmpty()) return ip
    if (ips.size == 1) return ips[0]
    val localInterfaces = getDeviceIP4sWithPrefixLength()
    for (ip in ips) {
        if (localInterfaces.any { (localIp, prefixLen) -> isSameSubnet(ip, localIp, prefixLen) }) {
            return ip
        }
    }
    return ips[0]
}

fun DPeer.getBaseUrl(): String = "https://${getBestIp()}:$port"

fun DPeer.getApiUrl(): String = "${getBaseUrl()}/peer_graphql"

fun DPeer.getStatusWsUrl(): String = "wss://${getBestIp()}:$port/status"

fun DPeer.getFileUrl(fileId: String): String = "${getBaseUrl()}/fs?id=${fileId.urlEncode()}"

fun DPeer.getName(): String {
    return name.ifBlank { getBestIp() }
}
