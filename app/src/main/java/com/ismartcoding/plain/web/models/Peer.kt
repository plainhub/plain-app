package com.ismartcoding.plain.web.models

import com.ismartcoding.plain.chat.PeerStatusManager
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.db.getBestIp
import kotlin.time.Instant

data class Peer(
    val id: String,
    val name: String,
    val ip: String,
    val status: String,
    val port: Int,
    val deviceType: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val online: Boolean,
)

fun DPeer.toModel(): Peer {
    return Peer(id, name, getBestIp(), status, port, deviceType, createdAt, updatedAt, PeerStatusManager.isOnline(id))
}
