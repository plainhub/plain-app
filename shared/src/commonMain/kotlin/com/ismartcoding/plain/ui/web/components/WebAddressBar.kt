package com.ismartcoding.plain.ui.web.components

import com.ismartcoding.plain.HttpServerManager
import com.ismartcoding.plain.preferences.PrefKeys

import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.ismartcoding.plain.lib.UrlHelper
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.collapse_backup_addresses
import com.ismartcoding.plain.i18n.collapsed
import com.ismartcoding.plain.i18n.expanded
import com.ismartcoding.plain.i18n.home_try_other_addresses
import com.ismartcoding.plain.i18n.change_port
import com.ismartcoding.plain.i18n.mdns_hostname
import com.ismartcoding.plain.i18n.show_qr_code
import com.ismartcoding.plain.platform.setClipboardText
import com.ismartcoding.plain.ui.components.PIconButton
import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.ui.base.PExpandableSection
import com.ismartcoding.plain.ui.base.VerticalSpace
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.pen as ui_drawable_pen
import com.ismartcoding.plain.ui.resources.qr_code as ui_drawable_qr_code

@Composable
fun WebAddressBar(
    isHttps: Boolean,
    lanIpv4Addresses: List<String>,
    modifier: Modifier = Modifier,
) {
    val port = if (isHttps) Prefs.httpsPort.collectAsState() else Prefs.httpPort.collectAsState()
    // Shared persisted state so the HTTP/HTTPS pager pages stay in sync.
    val expanded = Prefs.webAddressBarExpanded.collectAsState()
    var portDialogVisible by remember { mutableStateOf(false) }
    var qrCodeDialogVisible by remember { mutableStateOf(false) }
    var mdnsEditDialogVisible by remember { mutableStateOf(false) }
    val hostname by Prefs.mdnsHostname.collectAsState()
    var qrCodeUrl by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val scheme = if (isHttps) "https" else "http"
    val primaryAddress = lanIpv4Addresses.firstOrNull() ?: hostname
    val backupAddresses = (listOf(hostname) + lanIpv4Addresses.drop(1)).filter { it != primaryAddress }

    Column(modifier.fillMaxWidth()) {
        val primaryUrl = UrlHelper.buildUrl(scheme, primaryAddress, port.value)
        AddressRow(
            primaryUrl,
            primaryAddress == hostname,
            {
                if (primaryAddress == hostname) mdnsEditDialogVisible = true
                else portDialogVisible = true
            },
            {
                qrCodeUrl = primaryUrl
                qrCodeDialogVisible = true
            },
        )
        if (backupAddresses.isNotEmpty()) {
            val expansion = stringResource(if (expanded.value) Res.string.expanded else Res.string.collapsed)
            val text = stringResource(
                if (expanded.value) Res.string.collapse_backup_addresses
                else Res.string.home_try_other_addresses,
            )
            val toggle: () -> Unit = {
                scope.launch { Prefs.set(PrefKeys.WEB_ADDRESS_BAR_EXPANDED, !expanded.value) }
            }
            PExpandableSection(
                toggleModifier = Modifier.padding(start = 16.dp, end = 8.dp),
                text = text,
                expanded = expanded.value,
                stateDescription = expansion,
                onClick = toggle,
                content = {
                    Column {
                        backupAddresses.forEach { address ->
                            val url = UrlHelper.buildUrl(scheme, address, port.value)
                            AddressRow(
                                url,
                                address == hostname,
                                {
                                    if (address == hostname) {
                                        mdnsEditDialogVisible = true
                                    } else {
                                        portDialogVisible = true
                                    }
                                },
                                {
                                    qrCodeUrl = url
                                    qrCodeDialogVisible = true
                                },
                            )
                        }
                        VerticalSpace(8.dp)
                    }
                },
            )
        }
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
                mdnsEditDialogVisible = false
                scope.launch {
                    if (hostnameChanged) Prefs.setMdnsHostname(newHostname)
                    if (portChanged) {
                        if (isHttps) Prefs.set(PrefKeys.HTTPS_PORT, newPort)
                        else Prefs.set(PrefKeys.HTTP_PORT, newPort)
                    }
                    if (hostnameChanged || portChanged) HttpServerManager.restartHttpServer()
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
                HttpServerManager.restartHttpServer()
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
    isHostname: Boolean,
    onEdit: () -> Unit,
    onQr: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SelectionContainer(Modifier.weight(1f)) {
            Text(
                text = url,
                modifier = Modifier.fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .clickable {
                        setClipboardText("url", url)
                    }
                    .padding(vertical = 8.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false,
            )
        }
        PIconButton(
            icon = UiRes.drawable.ui_drawable_pen,
            modifier = Modifier.size(32.dp),
            iconSize = 16.dp,
            contentDescription = stringResource(if (isHostname) Res.string.mdns_hostname else Res.string.change_port),
            click = onEdit,
        )
        PIconButton(
            icon = UiRes.drawable.ui_drawable_qr_code,
            modifier = Modifier.size(32.dp),
            iconSize = 16.dp,
            contentDescription = stringResource(Res.string.show_qr_code),
            click = onQr,
        )
    }
}
