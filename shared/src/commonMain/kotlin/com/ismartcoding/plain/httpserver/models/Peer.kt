package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.enums.DeviceType
import com.ismartcoding.plain.enums.PeerStatus
import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Peer(
    val id: String,
    val name: String,
    val ip: String,
    val status: PeerStatus,
    val port: Int,
    val deviceType: DeviceType,
    val createdAt: Instant,
    val updatedAt: Instant,
    val online: Boolean,
)
