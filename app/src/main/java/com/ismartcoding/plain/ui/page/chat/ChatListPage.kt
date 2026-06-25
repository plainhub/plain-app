package com.ismartcoding.plain.ui.page.chat

import com.ismartcoding.plain.i18n.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.chat.channel.ChannelCacher
import com.ismartcoding.plain.chat.ChatCacher
import com.ismartcoding.plain.chat.peer.PeerCacher
import com.ismartcoding.plain.chat.peer.PeerStatusManager
import com.ismartcoding.plain.db.getBestIp
import com.ismartcoding.plain.enums.ButtonSize
import com.ismartcoding.plain.enums.DeviceType
import com.ismartcoding.plain.preferences.LocalWeb
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
import com.ismartcoding.plain.ui.nav.Routing
import com.ismartcoding.plain.ui.page.chat.components.CreateChannelDialog
import com.ismartcoding.plain.ui.page.chat.components.PeerListItem
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.theme.PlainTheme

@Composable
fun ChatListPage(
    navController: NavHostController,
    mainVM: MainViewModel,
    peerVM: PeerViewModel,
    channelVM: ChannelViewModel,
) {
    val context = LocalContext.current
    val pairedPeers = PeerCacher.pairedPeers.collectAsStateValue()
    val unpairedPeers = PeerCacher.unpairedPeers.collectAsStateValue()
    val webEnabled = LocalWeb.current
    val refreshState = rememberRefreshLayoutState {
        PeerStatusManager.reconnectNow("chat_list_pull_refresh")
        peerVM.load()
        channelVM.load()
        setRefreshState(RefreshContentState.Finished)
    }
    val channels = ChannelCacher.channels.collectAsStateValue()

    PScaffold(
        topBar = { TopBarChat(navController, channelVM, onNavigateBack = { navController.popBackStack() }) },
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
                    if (!webEnabled) PAlert(
                        description = stringResource(Res.string.web_service_required_for_chat),
                        AlertType.WARNING
                    ) {
                        PFilledButton(
                            text = stringResource(Res.string.enable_web_service),
                            buttonSize = ButtonSize.SMALL,
                            onClick = {
                                mainVM.enableHttpServer(context, true)
                            })
                    }
                }
                item {
                    PeerListItem(
                        title = stringResource(Res.string.local_chat),
                        desc = stringResource(Res.string.local_chat_desc),
                        icon = Res.drawable.bot,
                        latestChat = ChatCacher.getLatestChat("local"),
                        modifier = PlainTheme.getCardModifier(),
                        onClick = { navController.navigate(Routing.Chat("peer:local")) })
                }
                if (channels.isNotEmpty()) {
                    item {
                        VerticalSpace(dp = 16.dp)
                        Subtitle(stringResource(Res.string.channels))
                    }
                    itemsIndexed(items = channels.toList(), key = { _, i -> i.id }) { index, channel ->
                        PeerListItem(
                            title = channel.name,
                            desc = "",
                            icon = Res.drawable.hash,
                            latestChat = ChatCacher.getLatestChat(channel.id),
                            onClick = {
                                navController.navigate(Routing.Chat("channel:${channel.id}"))
                            },
                            modifier = PlainTheme.getCardModifier(index, channels.size)
                        )
                    }
                }
                val allPeers = pairedPeers + unpairedPeers
                if (allPeers.isNotEmpty()) {
                    item {
                        VerticalSpace(dp = 16.dp)
                        Subtitle(stringResource(Res.string.devices))
                    }
                    itemsIndexed(items = allPeers, key = { _, i -> i.id }) { index, peer ->
                        PeerListItem(
                            title = peer.name,
                            desc = if (peer.isPaired()) peer.getBestIp() else peer.ip,
                            icon = DeviceType.fromValue(peer.deviceType).getIcon(),
                            online = PeerCacher.getPeerOnlineStatus(peer.id),
                            latestChat = ChatCacher.getLatestChat(peer.id),
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

        CreateChannelDialog(
            channelVM.showCreateChannelDialog,
            onConfirm = {
                channelVM.createChannel(it)
            })
    }
}
