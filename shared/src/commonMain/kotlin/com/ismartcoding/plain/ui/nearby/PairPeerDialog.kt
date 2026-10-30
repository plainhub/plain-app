package com.ismartcoding.plain.ui.nearby

import com.ismartcoding.plain.features.nearby.PairingPhase
import com.ismartcoding.plain.features.nearby.PairingMode
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.ismartcoding.plain.ui.base.PTextButton
import com.ismartcoding.plain.ui.base.PFilledButton
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
import com.ismartcoding.plain.ui.components.TextFieldDialog
import com.ismartcoding.plain.ui.components.ToastManager

@Composable
fun PairPeerDialog(device: NearbyDevice, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    val failureText = stringResource(Res.string.pairing_failed)
    var mode by remember { mutableStateOf(PairingMode.MANAGE) }
    var generation by remember { mutableStateOf<String?>(null) }
    var submitting by remember { mutableStateOf(false) }
    var disposed by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val state = generation?.let { NearbyViewModel.pairingStates[it] }
    val phase = state?.phase
    DisposableEffect(Unit) {
        onDispose {
            disposed = true
            generation?.let { scope.launch(NonCancellable) { RustPairingRuntime.cancelGeneration(it) } }
        }
    }
    LaunchedEffect(phase) {
        when (phase) {
            PairingPhase.SUCCEEDED, PairingPhase.CANCELLED -> onDismiss()
            PairingPhase.FAILED -> {
                ToastManager.showErrorToast(failureText)
                onDismiss()
            }
            else -> Unit
        }
    }
    if (phase == PairingPhase.PASSWORD_REQUIRED) {
        TextFieldDialog(
            title = stringResource(Res.string.password),
            isPassword = true,
            errorText = error ?: "",
            validator = { !submitting },
            onDismissRequest = onDismiss,
            onConfirm = { value ->
                submitting = true
                scope.launch {
                    try { RustPairingRuntime.submit(generation!!, value) }
                    catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { error = failureText }
                    finally { submitting = false }
                }
            },
        )
        return
    }
    if (generation != null) return
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.dialogSheetBackground,
        title = { Text(stringResource(Res.string.pair_device)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(device.name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        PairingMode.MANAGE to Res.string.pair_manage_device,
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
            PFilledButton(
                text = stringResource(Res.string.pair),
                isLoading = submitting,
                onClick = {
                    submitting = true
                    error = null
                    scope.launch {
                        try {
                            withContext(NonCancellable) {
                                if (generation == null) {
                                    val result = RustPairingRuntime.start(device, mode)
                                    val value = result.generation
                                    generation = value
                                    if (disposed) RustPairingRuntime.cancelGeneration(value)
                                }
                            }
                        } catch (cancelled: CancellationException) { throw cancelled }
                        catch (_: Exception) { error = failureText }
                        finally { submitting = false }
                    }
                },
            )
        },
        dismissButton = { PTextButton(text = stringResource(Res.string.cancel), onClick = onDismiss) },
    )
}
