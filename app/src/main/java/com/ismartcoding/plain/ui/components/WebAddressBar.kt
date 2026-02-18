package com.ismartcoding.plain.ui.components

import android.content.ClipData
import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.clipboardManager
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.helpers.AppHelper
import com.ismartcoding.plain.helpers.QrCodeGenerateHelper
import com.ismartcoding.plain.preferences.HttpPortPreference
import com.ismartcoding.plain.preferences.HttpsPortPreference
import com.ismartcoding.plain.preferences.MdnsHostnamePreference
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.PDialogRadioRow
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.PTextField
import com.ismartcoding.plain.ui.base.RadioDialog
import com.ismartcoding.plain.ui.base.RadioDialogOption
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.MainViewModel
import com.ismartcoding.plain.web.HttpServerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WebAddressBar(
    context: Context,
    mainVM: MainViewModel,
    isHttps: Boolean,
) {
    val port = if (isHttps) TempData.httpsPort else TempData.httpPort
    var portDialogVisible by remember { mutableStateOf(false) }
    var qrCodeDialogVisible by remember { mutableStateOf(false) }
    var mdnsEditDialogVisible by remember { mutableStateOf(false) }
    var hostname by remember { mutableStateOf(TempData.mdnsHostname) }
    val ip4 = mainVM.ip4
    val ip4s = listOf(hostname) + mainVM.ip4s.ifEmpty { listOf("127.0.0.1") }
    val scope = rememberCoroutineScope()
    var qrCodeUrl by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = RoundedCornerShape(12.dp),
            )
            .padding(vertical = 8.dp)
    ) {
        for (ip in ip4s) {
            Row(
                Modifier.height(40.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val url = "${if (isHttps) "https" else "http"}://$ip:${port}"
                SelectionContainer {
                    ClickableText(
                        text = AnnotatedString(url),
                        modifier = Modifier.padding(start = 16.dp),
                        style =
                            TextStyle(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 18.sp,
                            ),
                        onClick = {
                            val clip = ClipData.newPlainText(LocaleHelper.getString(R.string.link), url)
                            clipboardManager.setPrimaryClip(clip)
                            DialogHelper.showTextCopiedMessage(url)
                        },
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                PIconButton(
                    icon = R.drawable.pen,
                    modifier = Modifier.size(32.dp),
                    iconSize = 16.dp,
                    contentDescription = stringResource(id = R.string.edit),
                    tint = MaterialTheme.colorScheme.onSurface,
                    click = {
                        if (ip == hostname) {
                            mdnsEditDialogVisible = true
                        } else {
                            portDialogVisible = true
                        }
                    },
                )
                PIconButton(
                    icon = R.drawable.qr_code,
                    modifier = Modifier.size(32.dp),
                    iconSize = 16.dp,
                    contentDescription = stringResource(id = R.string.qrcode),
                    tint = MaterialTheme.colorScheme.onSurface,
                    click = {
                        qrCodeUrl = url
                        qrCodeDialogVisible = true
                    },
                )
                HorizontalSpace(dp = 4.dp)
            }
        }
    }

    // Combined mDNS edit dialog: hostname + port
    if (mdnsEditDialogVisible) {
        val ports = if (isHttps) HttpServerManager.httpsPorts else HttpServerManager.httpPorts
        var editHostname by remember { mutableStateOf(hostname) }
        var selectedPort by remember { mutableStateOf(port) }
        var hostnameError by remember { mutableStateOf(false) }

        AlertDialog(
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.surface,
            onDismissRequest = { mdnsEditDialogVisible = false },
            title = {
                Text(
                    text = stringResource(id = R.string.edit),
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            text = {
                Column {
                    // mDNS hostname section
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
                        placeholder = hostname,
                        errorMessage = if (hostnameError) stringResource(id = R.string.mdns_hostname_invalid) else "",
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Port section
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
                        // Validate hostname
                        if (editHostname.isEmpty() || !editHostname.endsWith(".local")) {
                            hostnameError = true
                            return@Button
                        }

                        val hostnameChanged = editHostname != hostname
                        val portChanged = selectedPort != port

                        if (hostnameChanged) {
                            hostname = editHostname
                            TempData.mdnsHostname = editHostname
                            scope.launch {
                                withIO { MdnsHostnamePreference.putAsync(context, editHostname) }
                            }
                        }
                        if (portChanged) {
                            scope.launch(Dispatchers.IO) {
                                if (isHttps) {
                                    HttpsPortPreference.putAsync(context, selectedPort)
                                } else {
                                    HttpPortPreference.putAsync(context, selectedPort)
                                }
                            }
                        }

                        mdnsEditDialogVisible = false

                        if (hostnameChanged || portChanged) {
                            androidx.appcompat.app.AlertDialog.Builder(context)
                                .setTitle(R.string.restart_app_title)
                                .setMessage(R.string.restart_app_message)
                                .setPositiveButton(R.string.relaunch_app) { _, _ ->
                                    AppHelper.relaunch(context)
                                }
                                .setCancelable(false)
                                .create()
                                .show()
                        }
                    },
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { mdnsEditDialogVisible = false }) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
        )
    }

    // Port dialog for IP address rows
    if (portDialogVisible) {
        RadioDialog(
            title = stringResource(R.string.change_port),
            options =
                (if (isHttps) HttpServerManager.httpsPorts else HttpServerManager.httpPorts).map {
                    RadioDialogOption(
                        text = it.toString(),
                        selected = it == port,
                    ) {
                        scope.launch(Dispatchers.IO) {
                            if (isHttps) {
                                HttpsPortPreference.putAsync(context, it)
                            } else {
                                HttpPortPreference.putAsync(context, it)
                            }
                        }
                        androidx.appcompat.app.AlertDialog.Builder(context)
                            .setTitle(R.string.restart_app_title)
                            .setMessage(R.string.restart_app_message)
                            .setPositiveButton(R.string.relaunch_app) { _, _ ->
                                AppHelper.relaunch(context)
                            }
                            .setCancelable(false)
                            .create()
                            .show()
                    }
                },
        ) {
            portDialogVisible = false
        }
    }
    if (qrCodeDialogVisible) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surface,
            onDismissRequest = {
                qrCodeDialogVisible = false
            }, confirmButton = {
                Button(
                    onClick = {
                        qrCodeDialogVisible = false
                    }
                ) {
                    Text(stringResource(id = R.string.close))
                }
            }, title = {

            }, text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(id = R.string.scan_qrcode_to_access_web),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Image(
                        bitmap = QrCodeGenerateHelper.generate(qrCodeUrl, 300, 300).asImageBitmap(),
                        contentDescription = stringResource(id = R.string.qrcode),
                        modifier = Modifier
                            .size(300.dp)
                    )
                }
            })
    }
}