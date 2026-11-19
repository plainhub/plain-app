package com.ismartcoding.plain.ui.nearby

import com.ismartcoding.plain.features.nearby.PairingMode
import com.ismartcoding.plain.models.PairingErrorCode
import com.ismartcoding.plain.i18n.*
import org.jetbrains.compose.resources.StringResource

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

internal fun PairingErrorCode?.messageResource(): StringResource = when (this) {
    PairingErrorCode.TIMEOUT -> Res.string.pairing_expired
    PairingErrorCode.REJECTED -> Res.string.pairing_rejected
    PairingErrorCode.TOO_MANY_LOGIN_ATTEMPTS -> Res.string.pairing_too_many_attempts
    PairingErrorCode.INVALID_PASSWORD -> Res.string.invalid_password
    else -> Res.string.pairing_failed
}
