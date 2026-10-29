package com.ismartcoding.plain.ui.nearby

import com.ismartcoding.plain.platform.system.Capability
import com.ismartcoding.plain.platform.system.getDeviceCapabilities

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import com.ismartcoding.plain.models.NearbyDevice
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.models.Peer
import com.ismartcoding.plain.ui.components.ButtonSize
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.lib.extensions.toSortName
import com.ismartcoding.plain.platform.network.BleAvailability
import com.ismartcoding.plain.platform.network.bleAvailabilityFlow
import com.ismartcoding.plain.platform.system.openAppSettings
import com.ismartcoding.plain.ui.base.AlertType
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.components.NavigationBackIcon
import com.ismartcoding.plain.ui.base.PAlert
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTextButton
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.nearby.NearbyViewModel
import com.ismartcoding.plain.ui.chat.PeerViewModel
import com.ismartcoding.plain.ui.chat.components.NearbyDeviceItem
import org.jetbrains.compose.resources.stringResource
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.ismartcoding.plain.features.nearby.BleScanDriver
import com.ismartcoding.plain.features.nearby.RustNearbyDevices
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.platform.network.canRequestBluetoothAccess
import kotlinx.coroutines.CancellationException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbyPage(
    navController: NavHostController,
    peerVM: PeerViewModel,
) {
    val nearbyDevices by NearbyViewModel.nearbyDevices.collectAsState()
    val isDiscovering by NearbyViewModel.isDiscovering
    var isBleScanning by remember { mutableStateOf(false) }
    val bleAvailability by bleAvailabilityFlow.collectAsState()
    val peerDiscoverySupported = Capability.PEER_DISCOVERY in getDeviceCapabilities()
    val isSearching = isDiscovering || isBleScanning
    val peers by peerVM.peerList.collectAsState()
    LaunchedEffect(peerVM) { peerVM.observe() }

    LaunchedEffect(peerDiscoverySupported) {
        if (peerDiscoverySupported) {
            try { RustNearbyDevices.observeDiscovery() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { LogCat.e("Nearby discovery failed", error) }
        }
    }
    LaunchedEffect(bleAvailability) {
        if (bleAvailability == BleAvailability.READY) {
            isBleScanning = true
            try { BleScanDriver.scan() }
            finally { isBleScanning = false }
        }
    }

    PScaffold(
        topBar = {
            PTopAppBar(
                onNavigateBack = { navController.navigateUp() },
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
            if (Capability.BLE_DISCOVERY in getDeviceCapabilities() && bleAvailability != BleAvailability.READY) {
                item {
                    val description = when {
                        bleAvailability == BleAvailability.BLUETOOTH_OFF -> stringResource(Res.string.bluetooth_off)
                        !canRequestBluetoothAccess() && bleAvailability == BleAvailability.PERMISSION_DENIED ->
                            stringResource(Res.string.bluetooth_permission_denied)
                        else -> stringResource(Res.string.bluetooth_permission_required_for_nearby)
                    }
                    val openSettings = !canRequestBluetoothAccess() && bleAvailability != BleAvailability.UNKNOWN
                    PAlert(
                        description = description,
                        AlertType.WARNING,
                    ) {
                        PTextButton(
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
            // The list renders instantly from the discovery history cache;
            // the spinner only fills in for a genuinely empty first sweep.
            nearbySearchingItem(isSearching && nearbyDevices.isEmpty())
            nearbyDeviceListItems(nearbyDevices, peers)
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
    nearbyDevices: List<NearbyDevice>,
    peers: List<Peer>,
) {
    val sortedDevices = nearbyDevices.sortedBy { it.name.toSortName() }
    items(sortedDevices, key = { it.id }) { item ->
        val isPaired = peers.any { it.id == item.id && it.isPaired() }
        val status = NearbyViewModel.getStatus(item.id, isPaired)
        NearbyDeviceItem(
            item = item,
            status = status,
            bestIp = item.bestIp,
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
