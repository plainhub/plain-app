package com.ismartcoding.plain.ui.page.settings

import com.ismartcoding.plain.i18n.*

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.platform.BleDebugInfo
import com.ismartcoding.plain.platform.getBleDebugInfo
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.Subtitle
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BleDebugPage(navController: NavHostController) {
    var info by remember { mutableStateOf(getBleDebugInfo()) }

    // Refresh the snapshot periodically so runtime status (advertisingRunning)
    // stays current while the page is open.
    LaunchedEffect(Unit) {
        while (true) {
            delay(2000)
            info = getBleDebugInfo()
        }
    }

    PScaffold(
        topBar = {
            PTopAppBar(
                navController = navController,
                title = stringResource(Res.string.ble_debug),
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.padding(top = paddingValues.calculateTopPadding()),
        ) {
            item { TopSpace() }
            item { BleSupportCard(info) }
            item { VerticalSpace(dp = 16.dp) }
            item { BlePermissionsCard(info) }
            item { VerticalSpace(dp = 16.dp) }
            item { BleRuntimeStatusCard(info) }
            item { BottomSpace(paddingValues) }
        }
    }
}

@Composable
private fun BleSupportCard(info: BleDebugInfo) {
    Subtitle(stringResource(Res.string.ble_support))
    PCard {
        PListItem(
            title = stringResource(Res.string.ble_bluetooth_supported),
            value = booleanText(info.bluetoothSupported),
        )
        PListItem(
            title = stringResource(Res.string.ble_bluetooth_enabled),
            value = booleanText(info.bluetoothEnabled),
        )
        PListItem(
            title = stringResource(Res.string.ble_advertise_ready),
            subtitle = stringResource(Res.string.ble_advertise_ready_desc),
            value = booleanText(info.advertiseReady),
        )
    }
}

@Composable
private fun BlePermissionsCard(info: BleDebugInfo) {
    Subtitle(stringResource(Res.string.ble_permissions))
    PCard {
        PListItem(
            title = stringResource(Res.string.ble_scan_permission),
            value = booleanText(info.scanPermissionGranted),
        )
        PListItem(
            title = stringResource(Res.string.ble_connect_permission),
            value = booleanText(info.connectPermissionGranted),
        )
        PListItem(
            title = stringResource(Res.string.ble_advertise_permission),
            value = booleanText(info.advertisePermissionGranted),
        )
        PListItem(
            title = stringResource(Res.string.ble_location_permission),
            value = booleanText(info.locationPermissionGranted),
        )
        PListItem(
            title = stringResource(Res.string.ble_permission_granted),
            value = booleanText(info.blePermissionGranted),
        )
    }
}

@Composable
private fun BleRuntimeStatusCard(info: BleDebugInfo) {
    Subtitle(stringResource(Res.string.ble_runtime_status))
    PCard {
        PListItem(
            title = stringResource(Res.string.ble_advertising_running),
            value = booleanText(info.advertisingRunning),
        )
        PListItem(
            title = stringResource(Res.string.ble_client_id),
            value = info.clientId.ifEmpty { stringResource(Res.string.not_available) },
        )
        PListItem(
            title = stringResource(Res.string.ble_service_uuid),
            subtitle = info.serviceUuid.ifEmpty { stringResource(Res.string.not_available) },
        )
    }
}

@Composable
private fun booleanText(value: Boolean): String =
    stringResource(if (value) Res.string.yes else Res.string.no)
