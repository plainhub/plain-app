package com.ismartcoding.plain.features.nearby

import kotlinx.serialization.Serializable

@Serializable
enum class PairingPhase {
    STARTING,
    PASSWORD_REQUIRED,
    AUTHENTICATING,
    PAIRING,
    TRUST_REQUIRED,
    WAITING_CONFIRMATION,
    COMMITTING,
    SUCCEEDED,
    FAILED,
    CANCELLED,
}
