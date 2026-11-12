package com.ismartcoding.plain.features.nearby

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class MdnsPacketLog(
    val time: Instant,
    val direction: MdnsPacketDirection,
    val srcIp: String,
    val srcPort: Int,
    val dstIp: String,
    val dstPort: Int,
    val size: Int,
    val summary: String,
    val detail: String,
)
