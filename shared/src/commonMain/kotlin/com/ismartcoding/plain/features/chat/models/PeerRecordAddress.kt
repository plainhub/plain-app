package com.ismartcoding.plain.features.chat.models

import com.ismartcoding.plain.models.Peer

import com.ismartcoding.plain.features.chat.peer.RustPeerStore

suspend fun Peer.getFileUrl(fileId: String): String = RustPeerStore.fileUrl(this, fileId)

fun Peer.getBaseUrl(): String {
    val port = port ?: error("peer_address_unavailable")
    require(ip.isNotEmpty() && port > 0) { "peer_address_unavailable" }
    return com.ismartcoding.plain.lib.UrlHelper.buildUrl("https", ip, port)
}
