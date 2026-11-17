package com.ismartcoding.plain.features.chat.models


import com.ismartcoding.plain.models.PeerConnectionErrorCode
import com.ismartcoding.plain.models.BusinessException
import com.ismartcoding.plain.models.Peer

import com.ismartcoding.plain.features.chat.peer.RustPeerStore

suspend fun Peer.getFileUrl(fileId: String): String = RustPeerStore.fileUrl(this, fileId)

fun Peer.getBaseUrl(): String {
    val port = port ?: throw BusinessException(PeerConnectionErrorCode.PEER_ADDRESS_UNAVAILABLE.wire)
    if (!(ip.isNotEmpty() && port > 0)) throw BusinessException(PeerConnectionErrorCode.PEER_ADDRESS_UNAVAILABLE.wire)
    return com.ismartcoding.plain.lib.UrlHelper.buildUrl("https", ip, port)
}
