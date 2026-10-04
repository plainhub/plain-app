package com.ismartcoding.plain.data

import kotlinx.serialization.Serializable

@Serializable
data class DPairingTicket(
    val deviceId: String,
    val deviceName: String,
    val deviceIp: String,
    val devicePort: Int,
    val generation: String,
    val delayMs: Long,
)
