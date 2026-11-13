package com.ismartcoding.plain.features.chat.models

import com.ismartcoding.plain.models.Peer

import com.ismartcoding.plain.features.chat.peer.RustPeerStore

fun Peer.getBestIp(): String = address?.bestIp.orEmpty()
suspend fun Peer.getFileUrl(fileId: String): String = RustPeerStore.fileUrl(this, fileId)
