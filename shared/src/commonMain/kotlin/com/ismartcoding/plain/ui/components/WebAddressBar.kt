package com.ismartcoding.plain.ui.components

import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.helpers.UrlHelper
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.collapse_backup_addresses
import com.ismartcoding.plain.i18n.collapsed
import com.ismartcoding.plain.i18n.expanded
import com.ismartcoding.plain.i18n.try_more_addresses
import com.ismartcoding.plain.platform.isLanAddress
import com.ismartcoding.plain.platform.restartServer
import com.ismartcoding.plain.preferences.UserPrefs
import com.ismartcoding.plain.ui.base.PTextButton
import com.ismartcoding.plain.ui.theme.cardBackgroundNormal
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.chevron_down as ui_drawable_chevron_down
import com.ismartcoding.plain.ui.resources.chevron_up as ui_drawable_chevron_up

@Composable
fun WebAddressBar(
    isHttps: Boolean,
) {
    val port = if (isHttps) UserPrefs.httpsPort.collectAsState() else UserPrefs.httpPort.collectAsState()
    // Shared persisted state so the HTTP/HTTPS pager pages stay in sync.
    val expanded = UserPrefs.webAddressBarExpanded.collectAsState()
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
        val expansion = stringResource(if (expanded.value) Res.string.expanded else Res.string.collapsed)
        PTextButton(
            modifier = Modifier.padding(horizontal = 8.dp).semantics {
                stateDescription = expansion
            },
            text = stringResource(if (expanded.value) Res.string.collapse_backup_addresses else Res.string.try_more_addresses),
            icon = painterResource(
                if (expanded.value)
                    UiRes.drawable.ui_drawable_chevron_up
                else
                    UiRes.drawable.ui_drawable_chevron_down
            ),
            onClick = {
                scope.launch { UserPrefs.webAddressBarExpanded.set(!expanded.value) }
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
                mdnsEditDialogVisible = false
                scope.launch {
                    if (hostnameChanged) {
                        com.ismartcoding.plain.preferences.RustSystemState.setMdnsHostname(newHostname)
                        hostname = newHostname
                    }
                    if (portChanged) {
                        val patch = if (isHttps) com.ismartcoding.plain.preferences.UserSettingsPatch(httpsPort = newPort)
                            else com.ismartcoding.plain.preferences.UserSettingsPatch(httpPort = newPort)
                        com.ismartcoding.plain.preferences.PreferencesClient.local.patchUser(patch)
                    }
                    if (hostnameChanged || portChanged) restartServer()
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
