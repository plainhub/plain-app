package com.ismartcoding.plain.features.nearby

import kotlinx.serialization.Serializable

@Serializable
data class MdnsSnapshot(
    val runtimeId: String?,
    val revision: Long,
    val running: Boolean,
    val scanning: Boolean,
    val services: List<MdnsServiceSnapshot> = emptyList(),
    val packetsIn: List<MdnsPacketLog> = emptyList(),
    val packetsOut: List<MdnsPacketLog> = emptyList(),
)
