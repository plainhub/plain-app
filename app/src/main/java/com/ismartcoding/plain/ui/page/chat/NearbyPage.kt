package com.ismartcoding.plain.ui.page.chat

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.plain.chat.peer.PeerCacher
import com.ismartcoding.plain.data.DNearbyDevice
import com.ismartcoding.plain.data.DQrPairData
import com.ismartcoding.plain.enums.ButtonSize
import com.ismartcoding.plain.ui.base.AlertType
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.NavigationBackIcon
import com.ismartcoding.plain.ui.base.PAlert
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.Subtitle
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.models.NearbyItemStatus
import com.ismartcoding.plain.ui.models.NearbyViewModel
import com.ismartcoding.plain.ui.models.PeerViewModel
import com.ismartcoding.plain.ui.page.chat.components.NearbyDeviceItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbyPage(
    navController: NavHostController,
    peerVM: PeerViewModel,
    nearbyVM: NearbyViewModel = viewModel(),
) {
    val nearbyDevices = nearbyVM.nearbyDevices
    val isDiscovering by nearbyVM.isDiscovering
    val isBleScanning by nearbyVM.isBleScanning
    val blePermissionReady by nearbyVM.blePermissionReady
    val isSearching = isDiscovering || isBleScanning
    val pairedPeers by PeerCacher.pairedPeers.collectAsState()

    var showQrSheet by remember { mutableStateOf(false) }
    var qrData by remember { mutableStateOf<DQrPairData?>(null) }

    LaunchedEffect(Unit) {
        if (!isDiscovering) {
            nearbyVM.startDiscovering()
        }
        if (blePermissionReady && !isBleScanning) {
            nearbyVM.startBleScanning()
        }
    }

    LaunchedEffect(showQrSheet) {
        if (showQrSheet && qrData == null) {
            qrData = nearbyVM.getQrDataAsync()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (isDiscovering) nearbyVM.stopDiscovering()
            if (isBleScanning) nearbyVM.stopBleScanning()
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
                actions = {
//                    PIconButton(
//                        icon = Res.drawable.qr_code,
//                        contentDescription = stringResource(Res.string.show_qr_code),
//                        tint = MaterialTheme.colorScheme.onSurface,
//                    ) {
//                        qrData = null
//                        showQrSheet = true
//                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            if (!blePermissionReady) {
                item {
                    PAlert(
                        description = stringResource(Res.string.bluetooth_permission_required_for_nearby),
                        AlertType.WARNING,
                    ) {
                        PFilledButton(
                            text = stringResource(Res.string.grant_permission),
                            buttonSize = ButtonSize.SMALL,
                            onClick = { nearbyVM.requestBlePermission() },
                        )
                    }
                }
            }
            nearbySearchingItem(isSearching)
            nearbyDeviceListItems(nearbyDevices, nearbyVM, peerVM, pairedPeers)
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

    if (showQrSheet) {
        NearbyQrBottomSheet(
            qrData = qrData,
            onDismiss = { showQrSheet = false },
        )
    }
}

internal fun LazyListScope.nearbyDeviceListItems(
    nearbyDevices: List<DNearbyDevice>,
    nearbyVM: NearbyViewModel,
    peerVM: PeerViewModel,
    pairedPeers: List<com.ismartcoding.plain.db.DPeer>,
) {
    if (nearbyDevices.isNotEmpty()) {
        nearbyDevices.forEach { item ->
            item {
                val isPaired = pairedPeers.any { it.id == item.id }
                val status = nearbyVM.getStatus(item.id, isPaired)
                LaunchedEffect(status) {
                    if (status == NearbyItemStatus.COMPLETING && isPaired) {
                        nearbyVM.clearStatus(item.id)
                    }
                }
                NearbyDeviceItem(
                    item = item,
                    status = status,
                    onPairClick = { nearbyVM.startPairing(item) },
                    onUnpairClick = { nearbyVM.unpairDevice(item.id) },
                    onCancelClick = { nearbyVM.cancelPairing(item.id) },
                )
                VerticalSpace(8.dp)
            }
        }
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
