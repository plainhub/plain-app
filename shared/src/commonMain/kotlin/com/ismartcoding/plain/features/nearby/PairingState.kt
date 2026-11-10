package com.ismartcoding.plain.features.nearby

import kotlinx.serialization.Serializable

@Serializable
data class PairingState(
    val generation: String,
    val deviceId: String? = null,
    val phase: PairingPhase,
    val error: String? = null,
    val verificationCode: String? = null,
    val identityChanged: Boolean = false,
)
