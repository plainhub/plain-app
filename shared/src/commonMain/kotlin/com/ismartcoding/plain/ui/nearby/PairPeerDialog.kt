package com.ismartcoding.plain.ui.nearby

import com.ismartcoding.plain.features.nearby.PairingMode
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.ismartcoding.plain.ui.base.PTextButton
import com.ismartcoding.plain.ui.theme.dialogSheetBackground
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import com.ismartcoding.plain.models.NearbyDevice
import com.ismartcoding.plain.models.PairingProgressNotice
import com.ismartcoding.plain.features.nearby.RustPairingRuntime
import com.ismartcoding.plain.i18n.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.ui.components.RequestVerificationCodeCard

@Composable
internal fun PairPeerDialog(
    device: NearbyDevice,
    initialMode: PairingMode,
    qrCode: String? = null,
    start: suspend (NearbyDevice, PairingMode) -> Boolean = RustPairingRuntime::start,
    cancel: suspend (String) -> Boolean = RustPairingRuntime::cancel,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val failureText = stringResource(Res.string.pairing_failed)
    var ownsRequest by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    var disposed by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val state = NearbyViewModel.pairingViews[device.id]?.takeIf { ownsRequest || submitting || it.active }
    val phase = state?.step
    fun failPairing() {
        PairingProjection.failed(PairingProgressNotice(device.id, device.name))
        error = failureText
    }
    fun startPairing() {
        if (ownsRequest || submitting) return
        PairingProjection.starting(device.id)
        submitting = true
        error = null
        scope.launch {
            try {
                withContext(NonCancellable) {
                    ownsRequest = start(device, initialMode)
                    if (!ownsRequest) failPairing()
                    else if (disposed && NearbyViewModel.pairingViews[device.id]?.active == true) cancel(device.id)
                }
            } catch (canceled: CancellationException) { throw canceled }
            catch (_: Exception) { failPairing() }
            finally { submitting = false }
        }
    }
    LaunchedEffect(Unit) { startPairing() }
    DisposableEffect(Unit) {
        onDispose {
            disposed = true
            if (ownsRequest && NearbyViewModel.pairingViews[device.id]?.active == true) {
                scope.launch(NonCancellable) { cancel(device.id) }
            }
        }
    }
    LaunchedEffect(phase) {
        when (phase) {
            PairingStep.SUCCEEDED -> if (qrCode == null) onDismiss()
            PairingStep.CANCELED -> onDismiss()
            else -> Unit
        }
    }
    if (phase == PairingStep.PASSWORD_REQUIRED) {
        PairingPasswordPrompt(
            state,
            error = error,
            submitting = submitting,
            onDismiss = onDismiss,
            onConfirm = { value ->
                submitting = true
                scope.launch {
                    try { PairingProjection.starting(device.id); RustPairingRuntime.submit(device.id, initialMode, value) }
                    catch (canceled: CancellationException) { throw canceled }
                    catch (_: Exception) { error = failureText }
                    finally { submitting = false }
                }
            },
        )
        return
    }
    val waiting = ownsRequest || submitting || state?.active == true
    val localManageTrust = initialMode == PairingMode.MANAGE && phase == PairingStep.TRUST_REQUIRED
    val expired = state?.expired == true
    val stateError = stringResource(state?.error.messageResource())
    AlertDialog(
        modifier = Modifier.testTag("nearby-pair-dialog"),
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.dialogSheetBackground,
        title = { Text(stringResource(when {
            expired -> Res.string.pairing_expired
            phase == PairingStep.SUCCEEDED -> Res.string.paired
            waiting && phase != PairingStep.FAILED && !localManageTrust && qrCode == null -> Res.string.pairing_waiting_for_confirmation
            else -> Res.string.pair_device
        })) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(device.name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (waiting) {
                    if (localManageTrust) {
                        if (state.identityChanged) {
                            Text(stringResource(Res.string.pairing_manage_identity_changed), color = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        state?.comparisonCode(initialMode)?.takeIf { it.isNotEmpty() }?.let { code ->
                            RequestVerificationCodeCard(code, stringResource(Res.string.pairing_compare_code), Modifier.testTag("nearby-pair-code"))
                            if (state.identityChanged) Text(stringResource(Res.string.pairing_identity_changed), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                if (!expired) (error ?: stateError.takeIf { phase == PairingStep.FAILED })?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
                if (initialMode == PairingMode.MANAGE && phase == PairingStep.WAITING_FOR_PEER && !submitting) {
                    PTextButton(text = stringResource(Res.string.pair_another_device), onClick = {
                        ownsRequest = false
                        onDismiss()
                    })
                }
            }
        },
        confirmButton = {
            if (phase == PairingStep.TRUST_REQUIRED) PairingTrustButton(state, initialMode, submitting) {
                submitting = true
                scope.launch {
                    PairingProjection.trustAccepted(device.id)
                    try {
                        if (!RustPairingRuntime.respond(device.id, true)) failPairing()
                    } catch (canceled: CancellationException) { throw canceled }
                    catch (_: Exception) { failPairing() }
                    finally { submitting = false }
                }
            }
        },
        dismissButton = { PTextButton(text = stringResource(if (phase == PairingStep.SUCCEEDED || phase == PairingStep.FAILED) Res.string.close else Res.string.cancel), onClick = onDismiss) },
    )
}
