package com.ismartcoding.plain.ui.nearby

import com.ismartcoding.plain.features.nearby.PairingMode
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.ismartcoding.plain.ui.base.PTextButton
import com.ismartcoding.plain.ui.base.POutlinedButton
import com.ismartcoding.plain.ui.base.PDialogRadioRow
import com.ismartcoding.plain.ui.theme.dialogSheetBackground
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import com.ismartcoding.plain.models.NearbyDevice
import com.ismartcoding.plain.features.nearby.RustPairingRuntime
import com.ismartcoding.plain.i18n.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.ui.components.RequestVerificationCodeCard
import com.ismartcoding.plain.ui.components.TextFieldDialog
import com.ismartcoding.plain.ui.components.ToastManager

@Composable
fun PairPeerDialog(device: NearbyDevice, initialMode: PairingMode? = null, qrCode: String? = null, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    val failureText = stringResource(Res.string.pairing_failed)
    val directMode = initialMode ?: if (device.remoteAccess == true) null else PairingMode.CHAT
    var mode by remember { mutableStateOf(directMode ?: PairingMode.MANAGE) }
    var started by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    var disposed by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val state = NearbyViewModel.pairingViews[device.id]
    val phase = state?.step
    fun startPairing() {
        submitting = true
        error = null
        scope.launch {
            try {
                withContext(NonCancellable) {
                    if (!started) {
                        PairingProjection.starting(device.id)
                        RustPairingRuntime.start(device, mode)
                        started = true
                        if (disposed) RustPairingRuntime.cancel(device.id)
                    }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { error = failureText }
            finally { submitting = false }
        }
    }
    LaunchedEffect(Unit) { if (directMode != null) startPairing() }
    DisposableEffect(Unit) {
        onDispose {
            disposed = true
            if (started) scope.launch(NonCancellable) { RustPairingRuntime.cancel(device.id) }
        }
    }
    LaunchedEffect(phase) {
        when (phase) {
            PairingStep.SUCCEEDED -> if (qrCode == null) onDismiss()
            PairingStep.CANCELLED -> onDismiss()
            PairingStep.FAILED -> {
                ToastManager.showErrorToast(failureText)
                onDismiss()
            }
            else -> Unit
        }
    }
    if (phase == PairingStep.PASSWORD_REQUIRED) {
        TextFieldDialog(
            title = stringResource(Res.string.password),
            isPassword = true,
            errorText = error ?: "",
            validator = { !submitting },
            onDismissRequest = onDismiss,
            onConfirm = { value ->
                submitting = true
                scope.launch {
                    try { PairingProjection.starting(device.id); RustPairingRuntime.submit(device.id, mode, value) }
                    catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { error = failureText }
                    finally { submitting = false }
                }
            },
        )
        return
    }
    val waiting = started || submitting
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.dialogSheetBackground,
        title = { Text(stringResource(when {
            phase == PairingStep.SUCCEEDED -> Res.string.paired
            waiting && qrCode == null -> Res.string.waiting_for_confirmation
            else -> Res.string.pair_device
        })) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(device.name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (waiting) {
                    state?.verificationCode?.takeIf { it.isNotEmpty() }?.let { code ->
                        RequestVerificationCodeCard(code, stringResource(Res.string.pairing_compare_code))
                        if (state.identityChanged) Text(stringResource(Res.string.pairing_identity_changed), color = MaterialTheme.colorScheme.error)
                    }
                } else if (directMode == null) Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        PairingMode.MANAGE to Res.string.pair_full_control,
                        PairingMode.CHAT to Res.string.pair_chat_only,
                    ).forEach { (value, label) ->
                        PDialogRadioRow(
                            selected = mode == value,
                            onClick = { mode = value },
                            text = stringResource(label),
                            enabled = !submitting,
                        )
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            if (phase == PairingStep.TRUST_REQUIRED && !state.verificationCode.isNullOrEmpty()) POutlinedButton(
                text = stringResource(if (state.identityChanged) Res.string.pairing_trust else Res.string.confirm),
                isLoading = submitting,
                onClick = {
                    submitting = true
                    scope.launch {
                        try { RustPairingRuntime.respond(device.id, true) }
                        catch (cancelled: CancellationException) { throw cancelled }
                        catch (_: Exception) { error = failureText }
                        finally { submitting = false }
                    }
                },
            ) else if (!waiting) POutlinedButton(
                text = stringResource(Res.string.pair),
                isLoading = submitting,
                onClick = {
                    startPairing()
                },
            )
        },
        dismissButton = { PTextButton(text = stringResource(if (phase == PairingStep.SUCCEEDED) Res.string.close else Res.string.cancel), onClick = onDismiss) },
    )
}
