package com.ismartcoding.plain.ui.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.ui.models.MainViewModel
import com.ismartcoding.plain.ui.theme.cardBackgroundNormal

@Composable
fun WebAddressBar(
    context: Context,
    mainVM: MainViewModel,
    isHttps: Boolean,
    showIpAddresses: Boolean = true,
) {
    val port = if (isHttps) TempData.httpsPort else TempData.httpPort
    var portDialogVisible by remember { mutableStateOf(false) }
    var qrCodeDialogVisible by remember { mutableStateOf(false) }
    var mdnsEditDialogVisible by remember { mutableStateOf(false) }
    var hostname by remember { mutableStateOf(TempData.mdnsHostname) }
    var qrCodeUrl by remember { mutableStateOf("") }
    val ip4s = if (showIpAddresses) {
        listOf(hostname) + mainVM.ip4s.ifEmpty { listOf("127.0.0.1") }
    } else {
        listOf(hostname)
    }
    val scope = rememberCoroutineScope()

    Column(
        Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.cardBackgroundNormal,
                shape = RoundedCornerShape(12.dp),
            )
            .padding(vertical = 8.dp),
    ) {
        for (ip in ip4s) {
            val url = "${if (isHttps) "https" else "http"}://$ip:${port}"
            Row(
                modifier = Modifier.height(40.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WebAddressBarRow(
                    url = url,
                    isHostnameRow = ip == hostname,
                    onEditClick = {
                        if (ip == hostname) {
                            mdnsEditDialogVisible = true
                        } else {
                            portDialogVisible = true
                        }
                    },
                    onQrClick = {
                        qrCodeUrl = url
                        qrCodeDialogVisible = true
                    },
                )
            }
        }
    }

    if (mdnsEditDialogVisible) {
        MdnsAndPortEditDialog(
            isHttps = isHttps,
            currentHostname = hostname,
            currentPort = port,
            onDismiss = { mdnsEditDialogVisible = false },
            onSave = { newHostname, newPort ->
                val hostnameChanged = newHostname != hostname
                val portChanged = newPort != port
                if (hostnameChanged) {
                    hostname = newHostname
                    TempData.mdnsHostname = newHostname
                    persistMdnsHostname(context, scope, newHostname)
                }
                if (portChanged) {
                    persistPort(context, scope, isHttps, newPort)
                }
                mdnsEditDialogVisible = false
                if (hostnameChanged || portChanged) {
                    showRestartAppDialog(context)
                }
            },
        )
    }

    if (portDialogVisible) {
        PortSelectionDialog(
            isHttps = isHttps,
            currentPort = port,
            onDismiss = { portDialogVisible = false },
            onSelect = {
                persistPort(context, scope, isHttps, it)
                portDialogVisible = false
                showRestartAppDialog(context)
            },
        )
    }

    if (qrCodeDialogVisible) {
        WebAddressBarQrDialog(
            url = qrCodeUrl,
            onClose = { qrCodeDialogVisible = false },
        )
    }
}