package com.ismartcoding.plain.discover

data class MdnsPacketLog(
    val time: Long,
    val direction: MdnsPacketDirection,
    val srcIp: String,
    val srcPort: Int,
    val dstIp: String,
    val dstPort: Int,
    val size: Int,
    val summary: String,
    val detail: String,
)
