package com.ismartcoding.plain.ui.web

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.ismartcoding.plain.features.clipboard.ClipboardClient
import com.ismartcoding.plain.features.clipboard.ClipboardDevice
import com.ismartcoding.plain.features.clipboard.ClipboardDirection
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.base.PDropdownMenu
import com.ismartcoding.plain.ui.base.PDropdownMenuItem
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PTextButton
import com.ismartcoding.plain.ui.theme.dialogSheetBackground
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ClipboardDirectionSettingsDialog(onDismiss: () -> Unit) {
    val client = ClipboardClient.directions
    val scope = rememberCoroutineScope()
    var devices by remember { mutableStateOf<List<ClipboardDevice>?>(null) }
    var error by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var reload by remember { mutableIntStateOf(0) }
    LaunchedEffect(reload) {
        try {
            devices = client.devices()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            error = true
        }
    }
    AlertDialog(
        containerColor = MaterialTheme.colorScheme.dialogSheetBackground,
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text(stringResource(Res.string.clipboard_sync)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (error) Text(stringResource(Res.string.error), color = MaterialTheme.colorScheme.error)
                val rows = devices
                when {
                    rows == null && !error -> Text(stringResource(Res.string.loading))
                    rows?.isEmpty() == true -> Text(stringResource(Res.string.clipboard_no_managed_devices))
                    else -> rows.orEmpty().forEach { device ->
                        ClipboardDirectionRow(device, enabled = !saving) { direction ->
                            saving = true
                            error = false
                            scope.launch {
                                try {
                                    val confirmed = client.setDirection(device.id, direction)
                                    devices = devices?.map {
                                        if (it.id == device.id) it.copy(direction = confirmed) else it
                                    }
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (_: Exception) {
                                    error = true
                                } finally {
                                    saving = false
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { PTextButton(stringResource(Res.string.close), onDismiss, enabled = !saving) },
        dismissButton = {
            if (error) PTextButton(stringResource(Res.string.try_again), onClick = { error = false; devices = null; reload++ })
        },
    )
}

@Composable
private fun ClipboardDirectionRow(device: ClipboardDevice, enabled: Boolean, change: (ClipboardDirection) -> Unit) {
    var expanded by remember(device.id) { mutableStateOf(false) }
    Box {
        PListItem(
            modifier = Modifier.clickable(enabled = enabled) { expanded = true },
            title = device.name.ifBlank { device.id },
            value = directionText(device.direction),
            showMore = true,
        )
        PDropdownMenu(expanded = expanded && enabled, onDismissRequest = { expanded = false }) {
            ClipboardDirection.entries.forEach { direction ->
                PDropdownMenuItem(
                    text = { Text(directionText(direction)) },
                    onClick = {
                        expanded = false
                        if (direction != device.direction) change(direction)
                    },
                )
            }
        }
    }
}

@Composable
private fun directionText(direction: ClipboardDirection): String = stringResource(when (direction) {
    ClipboardDirection.OFF -> Res.string.off
    ClipboardDirection.SEND -> Res.string.clipboard_send_only
    ClipboardDirection.RECEIVE -> Res.string.clipboard_receive_only
    ClipboardDirection.BOTH -> Res.string.clipboard_both_directions
})
