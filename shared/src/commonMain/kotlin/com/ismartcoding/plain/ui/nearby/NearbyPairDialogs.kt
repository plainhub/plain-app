package com.ismartcoding.plain.ui.nearby

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.ismartcoding.plain.features.nearby.PairingMode
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.models.NearbyDevice
import com.ismartcoding.plain.models.PairingErrorCode
import com.ismartcoding.plain.ui.base.POutlinedButton
import com.ismartcoding.plain.ui.base.PTextButton
import com.ismartcoding.plain.ui.components.RadioDialog
import com.ismartcoding.plain.ui.components.RadioDialogOption
import com.ismartcoding.plain.ui.components.RequestVerificationCodeCard
import com.ismartcoding.plain.ui.components.TextFieldDialog
import com.ismartcoding.plain.ui.theme.dialogSheetBackground
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun NearbyPairModeDialog(name: String, onDismiss: () -> Unit, onMode: (PairingMode) -> Unit) {
    var mode by remember { mutableStateOf(PairingMode.MANAGE) }
    RadioDialog(
        modifier = Modifier.testTag("nearby-pair-dialog"),
        title = stringResource(Res.string.pair_with_device, name),
        description = stringResource(Res.string.pair_choose_connection),
        options = listOf(
            RadioDialogOption(
                text = stringResource(Res.string.pair_chat_only),
                selected = mode == PairingMode.CHAT,
                supportingText = stringResource(Res.string.pair_chat_description),
                onClick = { mode = PairingMode.CHAT },
            ),
            RadioDialogOption(
                text = stringResource(Res.string.pair_full_control),
                selected = mode == PairingMode.MANAGE,
                supportingText = stringResource(Res.string.pair_control_description),
                onClick = { mode = PairingMode.MANAGE },
            ),
        ),
        confirmText = stringResource(Res.string.pair_continue),
        onConfirm = { onMode(mode) },
        onDismissRequest = onDismiss,
    )
}

@Composable
internal fun NearbyPairChallenge(device: NearbyDevice, mode: PairingMode, state: PairingView) {
    val cancel = { NearbyViewModel.cancelPairing(device.id) }
    when (state.step) {
        PairingStep.PASSWORD_REQUIRED -> PairingPasswordPrompt(
            state,
            onDismiss = cancel,
            onConfirm = { NearbyViewModel.retryPairing(device, mode, it) },
        )
        PairingStep.TRUST_REQUIRED -> AlertDialog(
            onDismissRequest = cancel,
            containerColor = MaterialTheme.colorScheme.dialogSheetBackground,
            title = { Text(stringResource(Res.string.pair_device)) },
            text = {
                Column {
                    Text(device.name, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (state.identityChanged) Text(
                        text = stringResource(if (mode == PairingMode.MANAGE) Res.string.pairing_manage_identity_changed else Res.string.pairing_identity_changed),
                        color = MaterialTheme.colorScheme.error,
                    )
                    state.comparisonCode(mode)?.takeIf { it.isNotEmpty() }?.let {
                        RequestVerificationCodeCard(it, stringResource(Res.string.pairing_compare_code))
                    }
                }
            },
            confirmButton = { PairingTrustButton(state, mode) { NearbyViewModel.confirmPairing(device) } },
            dismissButton = { PTextButton(text = stringResource(Res.string.cancel), onClick = cancel) },
        )
        else -> Unit
    }
}

@Composable
internal fun PairingPasswordPrompt(
    state: PairingView,
    error: String? = null,
    submitting: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    TextFieldDialog(
        title = stringResource(Res.string.password),
        isPassword = true,
        confirmText = stringResource(Res.string.pair),
        errorText = error ?: if (state.error == PairingErrorCode.INVALID_PASSWORD) stringResource(Res.string.invalid_password) else "",
        validator = { !submitting },
        onDismissRequest = onDismiss,
        onConfirm = onConfirm,
    )
}

@Composable
internal fun PairingTrustButton(state: PairingView, mode: PairingMode, submitting: Boolean = false, onConfirm: () -> Unit) {
    if (mode == PairingMode.MANAGE || !state.verificationCode.isNullOrEmpty()) {
        POutlinedButton(
            text = stringResource(if (mode == PairingMode.MANAGE || state.identityChanged) Res.string.pairing_trust else Res.string.confirm),
            isLoading = submitting,
            onClick = onConfirm,
        )
    }
}
