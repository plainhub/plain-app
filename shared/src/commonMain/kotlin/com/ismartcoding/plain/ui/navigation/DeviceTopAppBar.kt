package com.ismartcoding.plain.ui.navigation

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.platform.system.getDeviceName
import com.ismartcoding.plain.ui.app.MainViewModel
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.chat.LocalPeerViewModel
import com.ismartcoding.plain.ui.tools.LocalManagedDeviceViewModel
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.chevron_down as ui_drawable_chevron_down
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DeviceTopAppBar(
    navController: NavHostController,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val selectedDevice = MainViewModel.selectedDevice.value
    val localName by Prefs.deviceName.collectAsState()
    val manager = LocalManagedDeviceViewModel.current
    val state = manager?.state?.collectAsState()?.value
    val info = state?.client?.state?.collectAsState()?.value
    val deviceName = if (selectedDevice == null) localName.ifEmpty { getDeviceName() }
        else info?.deviceName ?: LocalPeerViewModel.current.peersMap.collectAsState().value[selectedDevice.peerId]?.name.orEmpty()
    var showDevices by remember { mutableStateOf(false) }
    if (showDevices) DeviceSelectionSheet(navController, onDismiss = { showDevices = false })
    PTopAppBar(
        title = deviceName,
        onTitleClick = { showDevices = true },
        titleTrailing = {
            Icon(painterResource(UiRes.drawable.ui_drawable_chevron_down), contentDescription = stringResource(Res.string.select_a_device))
        },
        actions = actions,
    )
}
