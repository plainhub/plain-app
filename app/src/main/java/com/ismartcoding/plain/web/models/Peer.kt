package com.ismartcoding.plain.web.models

import com.ismartcoding.plain.db.DPeer
import kotlin.time.Instant
import kotlinx.serialization.Serializable

data class Peer(
    val id: String,
    val name: String,
    val ip: String,
    val status: String,
    val port: Int,
    val deviceType: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

fun DPeer.toModel(): Peer {
    return Peer(id, name, ip, status, port, deviceType, createdAt, updatedAt)
}
