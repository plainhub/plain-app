package com.ismartcoding.plain.models

import kotlinx.serialization.Serializable

@Serializable
data class PairingProgressNotice(
    val deviceId: String,
    val deviceName: String,
    val error: String? = null,
    val verificationCode: String? = null,
)
