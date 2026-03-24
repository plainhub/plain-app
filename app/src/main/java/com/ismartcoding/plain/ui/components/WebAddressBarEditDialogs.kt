package com.ismartcoding.plain.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.base.PDialogRadioRow
import com.ismartcoding.plain.ui.base.PTextField
import com.ismartcoding.plain.ui.base.RadioDialog
import com.ismartcoding.plain.ui.base.RadioDialogOption
import com.ismartcoding.plain.web.HttpServerManager

@Composable
fun MdnsAndPortEditDialog(
    isHttps: Boolean,
    currentHostname: String,
    currentPort: Int,
    onDismiss: () -> Unit,
    onSave: (hostname: String, port: Int) -> Unit,
) {
    val ports = if (isHttps) HttpServerManager.httpsPorts else HttpServerManager.httpPorts
    var editHostname by remember { mutableStateOf(currentHostname) }
    var selectedPort by remember { mutableStateOf(currentPort) }
    var hostnameError by remember { mutableStateOf(false) }

    AlertDialog(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.edit)) },
        text = {
            Column {
                Text(
                    text = stringResource(id = R.string.mdns_hostname),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                PTextField(
                    readOnly = false,
                    value = editHostname,
                    singleLine = true,
                    onValueChange = {
                        editHostname = it
                        hostnameError = false
                    },
                    placeholder = currentHostname,
                    errorMessage = if (hostnameError) stringResource(id = R.string.mdns_hostname_invalid) else "",
                )
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = stringResource(id = R.string.change_port),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                LazyColumn(modifier = Modifier.height(200.dp)) {
                    items(ports.toList()) { p ->
                        PDialogRadioRow(
                            selected = p == selectedPort,
                            onClick = { selectedPort = p },
                            text = p.toString(),
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val valid = editHostname.isNotBlank() && editHostname.endsWith(".local")
                    if (!valid) {
                        hostnameError = true
                        return@Button
                    }
                    onSave(editHostname, selectedPort)
                },
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.cancel))
            }
        },
    )
}

@Composable
fun PortSelectionDialog(
    isHttps: Boolean,
    currentPort: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
) {
    val ports = if (isHttps) HttpServerManager.httpsPorts else HttpServerManager.httpPorts
    RadioDialog(
        title = stringResource(R.string.change_port),
        options =
            ports.map {
                RadioDialogOption(
                    text = it.toString(),
                    selected = it == currentPort,
                ) { onSelect(it) }
            },
    ) { onDismiss() }
}
