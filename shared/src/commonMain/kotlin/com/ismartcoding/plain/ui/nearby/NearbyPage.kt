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
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.lib.extensions.toSortName
import com.ismartcoding.plain.platform.network.BleAvailability
import com.ismartcoding.plain.platform.network.bleAvailabilityFlow
import com.ismartcoding.plain.platform.network.localNetworkPermission
import com.ismartcoding.plain.ui.permissions.rememberNetworkPermission
import com.ismartcoding.plain.platform.system.openAppSettings
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.components.NavigationBackIcon
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.chat.PeerViewModel
import com.ismartcoding.plain.ui.chat.components.NearbyDeviceItem
import com.ismartcoding.plain.ui.chat.components.NearbyPairedDeviceItem
import com.ismartcoding.plain.ui.chat.components.CurrentDeviceIcon
import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.platform.system.getDeviceName
import com.ismartcoding.plain.platform.system.getDeviceType
import com.ismartcoding.plain.ui.app.MainViewModel
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.chat.components.PeerIconWithStatus
import com.ismartcoding.plain.ui.platform.system.getIcon
import com.ismartcoding.plain.ui.theme.PlainTheme
import com.ismartcoding.plain.ui.base.PSectionTitle
import org.jetbrains.compose.resources.stringResource
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.ismartcoding.plain.rust.native.PairingQr
import com.ismartcoding.plain.rust.native.RustNativeApi
import com.ismartcoding.plain.ui.components.PIconButton
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.qr_code as ui_drawable_qr_code
import com.ismartcoding.plain.ui.components.DialogHelper
import com.ismartcoding.plain.ui.web.components.WebAddressBarQrDialog
import kotlinx.coroutines.launch
import com.ismartcoding.plain.features.nearby.BleScanDriver
import com.ismartcoding.plain.features.nearby.PairingMode
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
    val lanPermission = remember { localNetworkPermission() }
    val lanGranted = lanPermission?.let { rememberNetworkPermission(it) }
    val isSearching = isDiscovering || isBleScanning
    val peersMap by peerVM.peersMap.collectAsState()
    val peers = peersMap.values.filter { it.isPaired() }.sortedBy { it.name.toSortName() }
    val attempt = NearbyViewModel.nearbyAttempt
    val attemptView = attempt?.first?.id?.let { NearbyViewModel.pairingViews[it] }
    val activeDevice = attempt?.first?.takeIf { attemptView?.active == true }
    val visibleDevices = if (activeDevice != null && nearbyDevices.none { it.id == activeDevice.id }) nearbyDevices + activeDevice else nearbyDevices
    val newDevices = visibleDevices.filter { device -> peers.none { it.id == device.id } }
    val scope = rememberCoroutineScope()
    var chooseMode by remember { mutableStateOf<NearbyDevice?>(null) }
    chooseMode?.let { device ->
        NearbyPairModeDialog(device.name, onDismiss = { chooseMode = null }) { mode ->
            chooseMode = null
            NearbyViewModel.startPairing(device, mode)
        }
    }
    if (attempt != null && attemptView != null) {
        NearbyPairChallenge(attempt.first, attempt.second, attemptView)
    }
    var upgradePeer by remember { mutableStateOf<NearbyDevice?>(null) }
    upgradePeer?.let { device ->
        PairPeerDialog(device, initialMode = PairingMode.MANAGE) { upgradePeer = null }
    }
    var pairingCode by remember { mutableStateOf<PairingQr?>(null) }
    pairingCode?.let { code ->
        WebAddressBarQrDialog(code.payload, verificationCode = code.verificationCode) { pairingCode = null }
    }
    LaunchedEffect(peerVM) { peerVM.observe() }

    LaunchedEffect(lanGranted?.value) {
        if (lanGranted?.value == false) return@LaunchedEffect
        try { RustNearbyDevices.observeDiscovery() }
        catch (canceled: CancellationException) { throw canceled }
        catch (error: Exception) { LogCat.e("Nearby discovery failed", error) }
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
                navigationIcon = {
                    NavigationBackIcon { navController.navigateUp() }
                },
                title = stringResource(Res.string.nearby_devices),
                actions = {
                    PIconButton(
                        icon = UiRes.drawable.ui_drawable_qr_code,
                        contentDescription = stringResource(Res.string.show_qr_code),
                        click = {
                            scope.launch {
                                try { pairingCode = RustNativeApi.pairingCode() }
                                catch (canceled: CancellationException) { throw canceled }
                                catch (error: Exception) { DialogHelper.showErrorDialog(error.message.orEmpty()) }
                            }
                        },
                    )
                },
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            if (lanGranted?.value == false) {
                item { NearbyLocalNetworkNotice(lanPermission) }
            }
            item(key = "local") {
                LocalDeviceItem()
                VerticalSpace(8.dp)
            }
            if (peers.isNotEmpty()) {
                item { PSectionTitle(stringResource(Res.string.paired_devices)) }
            }
            items(peers, key = { "paired:${it.id}" }) { peer ->
                NearbyPairedDeviceItem(peer, nearbyDevices.find { it.id == peer.id }) {
                    upgradePeer = it
                }
                VerticalSpace(8.dp)
            }
            if (lanGranted?.value != false) nearbySearchingItem(isSearching)
            nearbyDeviceListItems(newDevices, activeDevice?.id) { device ->
                if (device.remoteAccess == true) chooseMode = device
                else NearbyViewModel.startPairing(device, PairingMode.CHAT)
            }
            if (newDevices.isEmpty() && !isSearching) {
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
            if (Capability.BLE in getDeviceCapabilities() &&
                bleAvailability != BleAvailability.READY && bleAvailability != BleAvailability.UNSUPPORTED
            ) {
                item { NearbyBluetoothTip(bleAvailability) }
            }
            item {
                BottomSpace(paddingValues)
            }
        }
    }
}

@Composable
private fun LocalDeviceItem() {
    val localName by Prefs.deviceName.collectAsState()
    val isCurrent = MainViewModel.selectedDevice.value == null
    PListItem(
        modifier = PlainTheme.getCardModifier(),
        title = localName.ifEmpty { getDeviceName() },
        subtitle = stringResource(Res.string.this_device),
        start = { PeerIconWithStatus(getDeviceType().getIcon(), null) },
        titleSuffix = if (isCurrent) { { CurrentDeviceIcon() } } else null,
        action = {
            if (!isCurrent) PIconButton(
                Res.drawable.arrow_left_right,
                contentDescription = stringResource(Res.string.nearby_switch_device),
                click = { MainViewModel.selectDevice(null) },
            )
        },
    )
}

@Composable
private fun NearbyBluetoothTip(availability: BleAvailability) {
    val canRequest = canRequestBluetoothAccess()
    val openSettings = availability == BleAvailability.PERMISSION_DENIED && !canRequest
    val description = stringResource(
        when {
            availability == BleAvailability.BLUETOOTH_OFF -> Res.string.bluetooth_off
            openSettings -> Res.string.bluetooth_permission_denied
            else -> Res.string.nearby_bluetooth_tip
        }
    )
    val action = stringResource(if (openSettings) Res.string.open_settings else Res.string.nearby_bluetooth_enable)
    val linkStyle = TextLinkStyles(style = SpanStyle(color = MaterialTheme.colorScheme.primary))
    Text(
        text = buildAnnotatedString {
            append(description)
            if (canRequest || openSettings) {
                append(" ")
                withLink(LinkAnnotation.Clickable("bluetooth", linkStyle) {
                    if (openSettings) openAppSettings() else NearbyViewModel.requestBlePermission()
                }) { append(action) }
            }
        },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

internal fun LazyListScope.nearbyDeviceListItems(
    nearbyDevices: List<NearbyDevice>,
    activePeerId: String?,
    onPair: (NearbyDevice) -> Unit,
) {
    val sortedDevices = nearbyDevices.sortedBy { it.name.toSortName() }
    items(sortedDevices, key = { it.id }) { item ->
        NearbyDeviceItem(
            item = item,
            enabled = activePeerId == null || activePeerId == item.id,
            onPair = { onPair(item) },
        )
        VerticalSpace(8.dp)
    }
}

internal fun LazyListScope.nearbySearchingItem(isLoading: Boolean) {
    item {
        Box(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isLoading) CircularProgressIndicator(
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
