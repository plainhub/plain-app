package com.ismartcoding.plain.ui.page.chat

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.R
import com.ismartcoding.plain.enums.ButtonSize
import com.ismartcoding.plain.enums.DeviceType
import com.ismartcoding.plain.preferences.LocalWeb
import com.ismartcoding.plain.preferences.NearbyDiscoverablePreference
import com.ismartcoding.plain.preferences.dataFlow
import com.ismartcoding.plain.preferences.dataStore
import com.ismartcoding.plain.ui.base.AlertType
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PAlert
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.Subtitle
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.setRefreshState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.extensions.collectAsStateValue
import com.ismartcoding.plain.ui.models.ChannelViewModel
import com.ismartcoding.plain.ui.models.PeerViewModel
import com.ismartcoding.plain.ui.models.MainViewModel
import com.ismartcoding.plain.ui.models.addChannelMember
import com.ismartcoding.plain.ui.models.removeChannelMember
import com.ismartcoding.plain.ui.nav.Routing
import com.ismartcoding.plain.ui.page.chat.components.CreateChannelDialog
import com.ismartcoding.plain.ui.page.chat.components.ChannelMembersDialog
import com.ismartcoding.plain.ui.page.chat.components.RenameChannelDialog
import com.ismartcoding.plain.ui.page.chat.components.PeerListItem
import com.ismartcoding.plain.ui.page.chat.TopBarChat
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.theme.PlainTheme
import kotlinx.coroutines.flow.map

@Composable
fun ChatListPage(
    navController: NavHostController, mainVM: MainViewModel, peerVM: PeerViewModel,
    channelVM: ChannelViewModel,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pairedPeers = peerVM.pairedPeers
    val unpairedPeers = peerVM.unpairedPeers
    val webEnabled = LocalWeb.current
    val isAppInForeground = remember { mutableStateOf(true) }
    val isScreenOn = remember { mutableStateOf(true) }
    val isPageVisible = remember { mutableStateOf(true) }
    val isDiscoverable = remember { context.dataStore.dataFlow.map { NearbyDiscoverablePreference.get(it) } }.collectAsStateValue(initial = NearbyDiscoverablePreference.default)
    val refreshState = rememberRefreshLayoutState { peerVM.loadPeers(); setRefreshState(RefreshContentState.Finished) }
    var showCreateChannelDialog by channelVM.showCreateChannelDialog
    var renameChannelId by remember { mutableStateOf<String?>(null) }
    var renameChannelName by remember { mutableStateOf("") }
    var manageMembersChannelId by channelVM.manageMembersChannelId
    val channels = channelVM.channels.collectAsStateValue()

    ChatListPageEffects(peerVM, scope, isAppInForeground, isPageVisible, isScreenOn)

    PScaffold(
        topBar = { TopBarChat(navController, channelVM, onNavigateBack = { navController.popBackStack() }) },
    ) { paddingValues ->
        PullToRefresh(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding()), refreshLayoutState = refreshState
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item { TopSpace() }
                item {
                    if (!webEnabled) PAlert(
                        description = stringResource(id = R.string.web_service_required_for_chat),
                        AlertType.WARNING
                    ) { PFilledButton(text = stringResource(R.string.enable_web_service), buttonSize = ButtonSize.SMALL, onClick = { mainVM.enableHttpServer(context, true) }) }
                }
                item {
                    PeerListItem(
                        title = stringResource(R.string.local_chat),
                        desc = stringResource(R.string.local_chat_desc),
                        icon = R.drawable.bot,
                        latestChat = peerVM.getLatestChat("local"),
                        modifier = PlainTheme.getCardModifier(),
                        onClick = { navController.navigate(Routing.Chat("local")) })
                }
                if (channels.isNotEmpty()) {
                    item {
                        VerticalSpace(dp = 16.dp)
                        Subtitle(stringResource(R.string.channels))
                    }
                    itemsIndexed(items = channels.toList(), key = { _, i -> i.id }) { index, channel ->
                        PeerListItem(
                            title = channel.name,
                            desc = stringResource(R.string.channels),
                            icon = R.drawable.hash,
                            latestChat = peerVM.getLatestChat(channel.id),
                            onClick = {
                                navController.navigate(Routing.Chat("channel:${channel.id}"))
                            },
                            modifier = PlainTheme.getCardModifier(index, channels.size)
                        )
                    }
                }
                val allPeers = pairedPeers.toList() + unpairedPeers.toList()
                if (allPeers.isNotEmpty()) {
                    item {
                        VerticalSpace(dp = 16.dp)
                        Subtitle(stringResource(R.string.nearby_devices))
                    }
                    itemsIndexed(items = allPeers, key = { _, i -> i.id }) { index, peer ->
                        PeerListItem(
                            title = peer.name,
                            desc = if (peer.isPaired()) peer.getBestIp() else peer.ip,
                            icon = DeviceType.fromValue(peer.deviceType).getIcon(),
                            online = peerVM.getPeerOnlineStatus(peer.id),
                            latestChat = peerVM.getLatestChat(peer.id),
                            peerId = peer.id,
                            onDelete = { peerVM.removePeer(context, it) },
                            onClick = { navController.navigate(Routing.Chat("peer:${peer.id}")) },
                            modifier = PlainTheme.getCardModifier(index, allPeers.size)
                        )
                    }
                }
                item { BottomSpace(paddingValues) }
            }
        }

        if (showCreateChannelDialog) {
            CreateChannelDialog(onDismiss = { showCreateChannelDialog = false }, onConfirm = { showCreateChannelDialog = false; channelVM.createChannel(it) })
        }
        if (renameChannelId != null) {
            RenameChannelDialog(
                currentName = renameChannelName,
                onDismiss = { renameChannelId = null },
                onConfirm = { val id = renameChannelId!!; renameChannelId = null; channelVM.renameChannel(id, it) })
        }
        val managedChannel = manageMembersChannelId?.let { id -> channels.find { it.id == id } }
        if (managedChannel != null) {
            ChannelMembersDialog(
                channel = managedChannel,
                pairedPeers = peerVM.pairedPeers.toList(),
                onAddMember = { channelVM.addChannelMember(managedChannel.id, it) },
                onRemoveMember = { channelVM.removeChannelMember(managedChannel.id, it) },
                onDismiss = { manageMembersChannelId = null })
        }
    }
}
