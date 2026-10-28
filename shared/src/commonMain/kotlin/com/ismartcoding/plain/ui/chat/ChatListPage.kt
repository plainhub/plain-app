package com.ismartcoding.plain.ui.chat

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.ismartcoding.plain.features.chat.models.getBestIp
import com.ismartcoding.plain.features.chat.peer.PeerStatusClient
import com.ismartcoding.plain.ui.components.ButtonSize
import com.ismartcoding.plain.ui.platform.system.getIcon
import com.ismartcoding.plain.events.PermissionsResultEvent
import com.ismartcoding.plain.events.RequestPermissionsEvent
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.grant_permission
import com.ismartcoding.plain.i18n.local_chat
import com.ismartcoding.plain.i18n.local_chat_desc
import com.ismartcoding.plain.i18n.pair_chat_only
import com.ismartcoding.plain.i18n.peer_manage_access
import com.ismartcoding.plain.i18n.unpaired
import com.ismartcoding.plain.i18n.nearby_wifi_devices_required_for_chat
import com.ismartcoding.plain.lib.Channel
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.platform.network.BleAvailability
import com.ismartcoding.plain.platform.network.bleAvailabilityFlow
import com.ismartcoding.plain.platform.network.canRequestBluetoothAccess
import com.ismartcoding.plain.platform.network.wifiAwarePermission
import com.ismartcoding.plain.platform.system.Capability
import com.ismartcoding.plain.platform.system.getDeviceCapabilities
import com.ismartcoding.plain.platform.network.ensureBlePermissionAsync
import com.ismartcoding.plain.platform.permissions.isGranted
import com.ismartcoding.plain.platform.system.openAppSettings
import com.ismartcoding.plain.ui.base.AlertType
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PAlert
import com.ismartcoding.plain.ui.base.PTextButton
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.base.pullrefresh.setRefreshState
import com.ismartcoding.plain.ui.media.extensions.collectAsStateValue
import com.ismartcoding.plain.ui.navigation.Routing
import com.ismartcoding.plain.ui.navigation.MainNavScaffold
import com.ismartcoding.plain.ui.chat.components.CreateChannelDialog
import com.ismartcoding.plain.ui.chat.components.PeerListItem
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.bot as ui_drawable_bot
import com.ismartcoding.plain.ui.resources.hash as ui_drawable_hash
import androidx.compose.material3.MaterialTheme
import com.ismartcoding.plain.ui.theme.onWarningContainer
import com.ismartcoding.plain.ui.state.launchSafe
import org.jetbrains.compose.resources.stringResource

@Composable
fun ChatListPage(
    navController: NavHostController,
    peerVM: PeerViewModel,
    channelVM: ChannelViewModel,
    onTabSelected: ((Int) -> Unit)? = null,
) {
    val pairedPeers = peerVM.pairedPeers.collectAsStateValue()
    val unpairedPeers = peerVM.unpairedPeers.collectAsStateValue()
    val latestChats by peerVM.latestChats.collectAsState()
    LaunchedEffect(peerVM) { peerVM.observe() }
    LaunchedEffect(channelVM) { channelVM.observe() }
    val refreshState = rememberRefreshLayoutState {
        PeerStatusClient.reconnectNow("chat_list_pull_refresh")
        peerVM.load()
        channelVM.load()
        setRefreshState(RefreshContentState.Finished)
    }
    val channels = sortChatChannels(channelVM.channels.collectAsStateValue(), latestChats)

    val bleAvailability by bleAvailabilityFlow.collectAsState()
    val capabilities = getDeviceCapabilities()
    val awareSupported = Capability.WIFI_AWARE in capabilities
    val awarePermission = remember { wifiAwarePermission() }
    var awareReady by remember(awareSupported, awarePermission) { mutableStateOf(!awareSupported || awarePermission?.isGranted() == true) }
    val bleReady = Capability.BLE_DISCOVERY !in capabilities || bleAvailability == BleAvailability.READY
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        Channel.sharedFlow.collect { event ->
            when (event) {
                is PermissionsResultEvent -> {
                    val permission = awarePermission ?: return@collect
                    val key = permission.toSysPermission()
                    if (!event.map.containsKey(key)) return@collect
                    val granted = permission.isGranted()
                    awareReady = granted
                }
            }
        }
    }

    MainNavScaffold(
        selectedIndex = 1,
        onTabSelected = onTabSelected,
        topBar = { TopBarChat(navController, onCreateChannel = { channelVM.showCreateChannelDialog.value = true }) },
    ) { paddingValues ->
        PullToRefresh(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding()),
            refreshLayoutState = refreshState
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item { TopSpace() }
                item {
                    if (!bleReady || !awareReady) PAlert(
                        description = stringResource(Res.string.nearby_wifi_devices_required_for_chat),
                        type = AlertType.WARNING,
                    ) {
                        PTextButton(
                            text = stringResource(Res.string.grant_permission),
                            buttonSize = ButtonSize.SMALL,
                            contentColor = MaterialTheme.colorScheme.onWarningContainer,
                            onClick = {
                                when {
                                    !bleReady -> {
                                        if (canRequestBluetoothAccess()) {
                                            scope.launchSafe {
                                                ensureBlePermissionAsync()
                                                if (!awareReady) awarePermission?.let { sendEvent(RequestPermissionsEvent(it)) }
                                            }
                                        } else {
                                            openAppSettings()
                                        }
                                    }
                                    else -> awarePermission?.let { sendEvent(RequestPermissionsEvent(it)) }
                                }
                            },
                        )
                    }
                }
                item {
                    PeerListItem(
                        title = stringResource(Res.string.local_chat),
                        desc = stringResource(Res.string.local_chat_desc),
                        icon = UiRes.drawable.ui_drawable_bot,
                        latestChat = latestChats["local"],
                        onClick = { navController.navigate(Routing.Chat("peer:local")) })
                }
                if (channels.isNotEmpty()) {
                    items(items = channels, key = { it.id }) { channel ->
                        PeerListItem(
                            title = channel.name,
                            desc = "",
                            icon = UiRes.drawable.ui_drawable_hash,
                            latestChat = latestChats[channel.id],
                            onClick = {
                                navController.navigate(Routing.Chat("channel:${channel.id}"))
                            },
                        )
                    }
                }
                val allPeers = pairedPeers + unpairedPeers
                if (allPeers.isNotEmpty()) {
                    items(items = allPeers, key = { it.id }) { peer ->
                        PeerListItem(
                            title = peer.name,
                            desc = if (peer.isPaired()) peer.getBestIp() else peer.ip,
                            icon = peer.deviceType.getIcon(),
                            online = peer.online,
                            accessLabel = stringResource(when {
                                !peer.isPaired() -> Res.string.unpaired
                                peer.token.isNotEmpty() -> Res.string.peer_manage_access
                                else -> Res.string.pair_chat_only
                            }),
                            latestChat = latestChats[peer.id],
                            peerId = peer.id,
                            onDelete = { peerVM.removePeer(it) },
                            onClick = { navController.navigate(Routing.Chat("peer:${peer.id}")) },
                        )
                    }
                }
                item { BottomSpace(paddingValues) }
            }
        }

        CreateChannelDialog(
            channelVM.showCreateChannelDialog,
            onConfirm = {
                channelVM.createChannel(it)
            })
    }
}
