package com.ismartcoding.plain.web.models

import com.ismartcoding.plain.enums.DeviceType
import kotlin.time.Instant

data class Peer(
    val id: String,
    val name: String,
    val ip: String,
    val status: String,
    val port: Int,
    val deviceType: DeviceType,
    val createdAt: Instant,
    val updatedAt: Instant,
    val online: Boolean,
)
