package com.ismartcoding.plain.ui.nearby

enum class PairingStep { PAIRING, PASSWORD_REQUIRED, TRUST_REQUIRED, SUCCEEDED, FAILED, CANCELLED }

data class PairingView(
    val peerId: String,
    val step: PairingStep,
    val error: String? = null,
    val verificationCode: String? = null,
    val identityChanged: Boolean = false,
)
