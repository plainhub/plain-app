package com.ismartcoding.plain.ui.chat

import androidx.compose.ui.unit.dp

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.ismartcoding.plain.features.chat.peer.PeerStatusClient
import com.ismartcoding.plain.ui.platform.system.getIcon
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.local_chat
import com.ismartcoding.plain.i18n.local_chat_desc
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
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
import org.jetbrains.compose.resources.stringResource

@Composable
fun ChatListPage(
    navController: NavHostController,
    peerVM: PeerViewModel,
    channelVM: ChannelViewModel,
    onTabSelected: ((Int) -> Unit)? = null,
    observeUpdates: Boolean = true,
) {
    val local = com.ismartcoding.plain.ui.navigation.LocalContentSource.current is com.ismartcoding.plain.rust.graphql.ContentSource.Local
    val allPeers = peerVM.peerList.collectAsStateValue()
    val latestChats by peerVM.latestChats.collectAsState()
    LaunchedEffect(peerVM, observeUpdates) { if (observeUpdates) peerVM.observe() }
    LaunchedEffect(channelVM, observeUpdates) { if (observeUpdates) channelVM.observe() }
    val refreshState = rememberRefreshLayoutState {
        if (local) PeerStatusClient.reconnectNow("chat_list_pull_refresh")
        peerVM.load()
        channelVM.load()
        setRefreshState(RefreshContentState.Finished)
    }
    val channels = sortChatChannels(channelVM.channels.collectAsStateValue(), latestChats)

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
                    PeerListItem(
                        title = stringResource(Res.string.local_chat),
                        desc = stringResource(Res.string.local_chat_desc),
                        icon = UiRes.drawable.ui_drawable_bot,
                        latestChat = latestChats["local"],
                        onClick = { navController.navigate(Routing.Chat("peer:local")) })
                    VerticalSpace(8.dp)
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
                        VerticalSpace(8.dp)
                    }
                }
                if (allPeers.isNotEmpty()) {
                    items(items = allPeers, key = { it.id }) { peer ->
                        PeerListItem(
                            title = peer.name,
                            desc = peer.ip,
                            icon = peer.deviceType.getIcon(),
                            online = peer.online,
                            latestChat = latestChats[peer.id],
                            peerId = peer.id,
                            onDelete = { peerVM.removePeer(it) },
                            onClick = { navController.navigate(Routing.Chat("peer:${peer.id}")) },
                        )
                        VerticalSpace(8.dp)
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
