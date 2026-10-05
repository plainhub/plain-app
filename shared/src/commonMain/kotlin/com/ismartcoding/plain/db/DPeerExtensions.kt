package com.ismartcoding.plain.db

import com.ismartcoding.plain.chat.peer.RustPeerStore

fun DPeer.getBestIp(): String = address?.bestIp.orEmpty()
fun DPeer.getBaseUrl(): String = address?.baseUrl.orEmpty()
fun DPeer.getApiUrl(): String = address?.apiUrl.orEmpty()
fun DPeer.getStatusWsUrl(): String = address?.statusWsUrl.orEmpty()
suspend fun DPeer.getFileUrl(fileId: String): String = RustPeerStore.fileUrl(this, fileId)
fun DPeer.getName(): String = address?.name ?: name
