package com.ismartcoding.plain.ui.nearby

import com.ismartcoding.plain.features.nearby.PairingMode
import com.ismartcoding.plain.models.PairingErrorCode

enum class PairingStep { PAIRING, PASSWORD_REQUIRED, TRUST_REQUIRED, WAITING_FOR_PEER, SUCCEEDED, FAILED, CANCELED }

data class PairingView(
    val peerId: String,
    val step: PairingStep,
    val error: PairingErrorCode? = null,
    val verificationCode: String? = null,
    val identityChanged: Boolean = false,
) {
    val active: Boolean
        get() = when (step) {
            PairingStep.PAIRING, PairingStep.PASSWORD_REQUIRED, PairingStep.TRUST_REQUIRED, PairingStep.WAITING_FOR_PEER -> true
            PairingStep.SUCCEEDED, PairingStep.FAILED, PairingStep.CANCELED -> false
        }

    val expired: Boolean
        get() = step == PairingStep.FAILED && error == PairingErrorCode.TIMEOUT

    fun comparisonCode(mode: PairingMode): String? =
        verificationCode?.takeUnless { step == PairingStep.TRUST_REQUIRED && mode == PairingMode.MANAGE }
}
