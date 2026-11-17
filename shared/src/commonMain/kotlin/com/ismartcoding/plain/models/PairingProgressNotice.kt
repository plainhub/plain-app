package com.ismartcoding.plain.models


import kotlinx.serialization.Serializable

@Serializable
data class PairingProgressNotice(
    val deviceId: String,
    val deviceName: String,
    val error: PairingErrorCode? = null,
    val verificationCode: String? = null,
)
