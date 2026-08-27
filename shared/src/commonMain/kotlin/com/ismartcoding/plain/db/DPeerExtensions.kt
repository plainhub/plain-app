package com.ismartcoding.plain.db

import com.ismartcoding.plain.platform.bestLanIp
import com.ismartcoding.plain.platform.getDeviceIP4sWithPrefixLength
import com.ismartcoding.plain.helpers.UrlHelper
import com.ismartcoding.plain.lib.extensions.urlEncode

fun DPeer.getBestIp(): String {
    val ips = getIpList()
    if (ips.isEmpty()) return ip
    return bestLanIp(ips, getDeviceIP4sWithPrefixLength())
}

fun DPeer.getBaseUrl(): String = UrlHelper.buildUrl("https", getBestIp(), port)

fun DPeer.getApiUrl(): String = "${getBaseUrl()}/peer_graphql"

fun DPeer.getStatusWsUrl(): String = UrlHelper.buildUrl("wss", getBestIp(), port, "/status")

fun DPeer.getFileUrl(fileId: String): String = "${getBaseUrl()}/fs?id=${fileId.urlEncode()}"

fun DPeer.getName(): String {
    return name.ifBlank { getBestIp() }
}
