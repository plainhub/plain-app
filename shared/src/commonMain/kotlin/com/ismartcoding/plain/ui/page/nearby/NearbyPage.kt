package com.ismartcoding.plain.ui.page.nearby

import com.ismartcoding.plain.i18n.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.chat.peer.PeerCacher
import com.ismartcoding.plain.data.DNearbyDevice
import com.ismartcoding.plain.enums.ButtonSize
import com.ismartcoding.plain.lib.extensions.toSortName
import com.ismartcoding.plain.platform.BleAvailability
import com.ismartcoding.plain.platform.bleAvailabilityFlow
import com.ismartcoding.plain.platform.getBestIp
import com.ismartcoding.plain.platform.isIOS
import com.ismartcoding.plain.platform.openAppSettings
import com.ismartcoding.plain.ui.base.AlertType
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.NavigationBackIcon
import com.ismartcoding.plain.ui.base.PAlert
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.models.NearbyViewModel
import com.ismartcoding.plain.ui.models.PeerViewModel
import com.ismartcoding.plain.ui.page.chat.components.NearbyDeviceItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbyPage(
    navController: NavHostController,
    peerVM: PeerViewModel,
) {
    val nearbyDevices by NearbyViewModel.nearbyDevices.collectAsState()
    val isDiscovering by NearbyViewModel.isDiscovering
    val isBleScanning by NearbyViewModel.isBleScanning
    val bleAvailability by bleAvailabilityFlow.collectAsState()
    val isSearching = isDiscovering || isBleScanning
    val pairedPeers by PeerCacher.pairedPeers.collectAsState()

    LaunchedEffect(Unit) {
        if (!isDiscovering) {
            NearbyViewModel.startDiscovering()
        }
    }
    LaunchedEffect(bleAvailability) {
        if (bleAvailability == BleAvailability.READY && !isBleScanning) {
            NearbyViewModel.startBleScanning()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (isDiscovering) NearbyViewModel.stopDiscovering()
            if (isBleScanning) NearbyViewModel.stopBleScanning()
        }
    }


    PScaffold(
        topBar = {
            PTopAppBar(
                navController = navController,
                navigationIcon = {
                    NavigationBackIcon { navController.navigateUp() }
                },
                title = stringResource(Res.string.nearby_devices),
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            if (bleAvailability != BleAvailability.READY && bleAvailability != BleAvailability.UNSUPPORTED) {
                item {
                    val description = when {
                        bleAvailability == BleAvailability.BLUETOOTH_OFF -> stringResource(Res.string.bluetooth_off)
                        isIOS() && bleAvailability == BleAvailability.PERMISSION_DENIED ->
                            stringResource(Res.string.bluetooth_permission_denied)
                        else -> stringResource(Res.string.bluetooth_permission_required_for_nearby)
                    }
                    // iOS has no in-app permission request or Bluetooth toggle:
                    // once denied/off, the only recovery is the system Settings
                    // app. Android keeps launching its own request dialogs.
                    val openSettings = isIOS() && bleAvailability != BleAvailability.UNKNOWN
                    PAlert(
                        description = description,
                        AlertType.WARNING,
                    ) {
                        PFilledButton(
                            text = stringResource(
                                when {
                                    openSettings -> Res.string.open_settings
                                    bleAvailability == BleAvailability.BLUETOOTH_OFF -> Res.string.enable
                                    else -> Res.string.grant_permission
                                }
                            ),
                            buttonSize = ButtonSize.SMALL,
                            onClick = {
                                if (openSettings) {
                                    openAppSettings()
                                } else {
                                    NearbyViewModel.requestBlePermission()
                                }
                            },
                        )
                    }
                }
            }
            nearbySearchingItem(isSearching)
            nearbyDeviceListItems(nearbyDevices, peerVM, pairedPeers)
            if (nearbyDevices.isEmpty() && !isSearching) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stringResource(Res.string.make_sure_devices_same_network),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                        }
                    }
                }
            }
            item {
                BottomSpace(paddingValues)
            }
        }
    }
}

internal fun LazyListScope.nearbyDeviceListItems(
    nearbyDevices: List<DNearbyDevice>,
    peerVM: PeerViewModel,
    pairedPeers: List<com.ismartcoding.plain.db.DPeer>,
) {
    val sortedDevices = nearbyDevices.sortedBy { it.name.toSortName() }
    items(sortedDevices, key = { it.id }) { item ->
        val isPaired = pairedPeers.any { it.id == item.id }
        val status = NearbyViewModel.getStatus(item.id, isPaired)
        NearbyDeviceItem(
            item = item,
            status = status,
            bestIp = getBestIp(item.ips),
        )
        VerticalSpace(8.dp)
    }
}

internal fun LazyListScope.nearbySearchingItem(isLoading: Boolean) {
    if (!isLoading) return
    item {
        Box(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                HorizontalSpace(8.dp)
                Text(
                    text = stringResource(Res.string.searching_nearby_devices),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
