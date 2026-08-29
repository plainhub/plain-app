package com.ismartcoding.plain.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.helpers.UrlHelper
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.platform.isLanAddress
import com.ismartcoding.plain.platform.restartServer
import com.ismartcoding.plain.preferences.WebAddressBarExpandedPreference
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.theme.cardBackgroundNormal
import com.ismartcoding.plain.ui.theme.tipsText
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun WebAddressBar(
    isHttps: Boolean,
) {
    val port = if (isHttps) TempData.httpsPort.collectAsState() else TempData.httpPort.collectAsState()
    // Shared persisted state so the HTTP/HTTPS pager pages stay in sync.
    val expanded = TempData.webAddressBarExpanded.collectAsState()
    var portDialogVisible by remember { mutableStateOf(false) }
    var qrCodeDialogVisible by remember { mutableStateOf(false) }
    var mdnsEditDialogVisible by remember { mutableStateOf(false) }
    var hostname by remember { mutableStateOf(TempData.mdnsHostname) }
    var qrCodeUrl by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val scheme = if (isHttps) "https" else "http"
    val ipList = TempData.ip4s.value.ifEmpty { listOf("127.0.0.1") }
    val primaryIp = ipList.firstOrNull { isLanAddress(it) } ?: ipList.firstOrNull() ?: "127.0.0.1"
    val backupIps = listOf(hostname) + ipList.filter { it != primaryIp }

    Column(
        Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.cardBackgroundNormal,
                shape = RoundedCornerShape(12.dp),
            )
            .padding(vertical = 8.dp),
    ) {
        val primaryUrl = UrlHelper.buildUrl(scheme, primaryIp, port.value)
        AddressRow(
            url = primaryUrl,
            isHostnameRow = false,
            onEditClick = { portDialogVisible = true },
            onQrClick = {
                qrCodeUrl = primaryUrl
                qrCodeDialogVisible = true
            },
        )
        AnimatedVisibility(
            visible = expanded.value,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column {
                backupIps.forEach { ip ->
                    val url = UrlHelper.buildUrl(scheme, ip, port.value)
                    AddressRow(
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
        ExpandCollapseBar(
            expanded = expanded.value,
            onClick = {
                // Capture the target value first: re-reading expanded.value inside the
                // coroutine would race with the synchronous update above and revert it.
                val newValue = !expanded.value
                TempData.webAddressBarExpanded.value = newValue
                scope.launch { WebAddressBarExpandedPreference.putAsync(newValue) }
            },
        )
    }

    if (mdnsEditDialogVisible) {
        MdnsAndPortEditDialog(
            isHttps = isHttps,
            currentHostname = hostname,
            currentPort = port.value,
            onDismiss = { mdnsEditDialogVisible = false },
            onSave = { newHostname, newPort ->
                val hostnameChanged = newHostname != hostname
                val portChanged = newPort != port.value
                if (hostnameChanged) {
                    hostname = newHostname
                    TempData.mdnsHostname = newHostname
                    persistMdnsHostname(scope, newHostname)
                }
                if (portChanged) {
                    persistPort(scope, isHttps, newPort)
                }
                mdnsEditDialogVisible = false
                if (hostnameChanged || portChanged) {
                    restartServer()
                }
            },
        )
    }

    if (portDialogVisible) {
        PortSelectionDialog(
            isHttps = isHttps,
            currentPort = port.value,
            onDismiss = { portDialogVisible = false },
            onSelect = {
                persistPort(scope, isHttps, it)
                portDialogVisible = false
                restartServer()
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

@Composable
private fun AddressRow(
    url: String,
    isHostnameRow: Boolean,
    onEditClick: () -> Unit,
    onQrClick: () -> Unit,
) {
    Row(
        modifier = Modifier.height(40.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WebAddressBarRow(
            url = url,
            isHostnameRow = isHostnameRow,
            onEditClick = onEditClick,
            onQrClick = onQrClick,
        )
    }
}

@Composable
private fun ExpandCollapseBar(expanded: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .height(40.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(if (expanded) Res.string.collapse_backup_addresses else Res.string.try_more_addresses),
            style = MaterialTheme.typography.tipsText(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalSpace(dp = 4.dp)
        Icon(
            painter = painterResource(if (expanded) Res.drawable.chevron_up else Res.drawable.expand_more),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
