package com.ismartcoding.plain.db

import com.ismartcoding.lib.extensions.urlEncode
import com.ismartcoding.lib.helpers.NetworkHelper

fun DPeer.getBestIp(): String {
    val ips = getIpList()
    if (ips.isEmpty()) return ip
    return NetworkHelper.getBestIp(ips)
}

fun DPeer.getBaseUrl(): String = "https://${getBestIp()}:$port"

fun DPeer.getApiUrl(): String = "${getBaseUrl()}/peer_graphql"

fun DPeer.getFileUrl(fileId: String): String = "${getBaseUrl()}/fs?id=${fileId.urlEncode()}"
