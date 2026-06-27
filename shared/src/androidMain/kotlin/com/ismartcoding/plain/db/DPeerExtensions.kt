package com.ismartcoding.plain.db

import com.ismartcoding.plain.lib.extensions.urlEncode
import com.ismartcoding.plain.lib.helpers.NetworkHelper

fun DPeer.getBestIp(): String {
    val ips = getIpList()
    if (ips.isEmpty()) return ip
    return NetworkHelper.getBestIp(ips)
}

fun DPeer.getBaseUrl(): String = "https://${getBestIp()}:$port"

fun DPeer.getApiUrl(): String = "${getBaseUrl()}/peer_graphql"

fun DPeer.getStatusWsUrl(): String = "wss://${getBestIp()}:$port/status"

fun DPeer.getFileUrl(fileId: String): String = "${getBaseUrl()}/fs?id=${fileId.urlEncode()}"

fun DPeer.getName(): String {
    return name.ifBlank { getBestIp() }
}