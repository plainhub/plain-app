package com.ismartcoding.plain.ui.page.root.contents

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.R
import com.ismartcoding.plain.enums.DeviceType
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.Subtitle
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.models.ChatListViewModel
import com.ismartcoding.plain.ui.nav.Routing
import com.ismartcoding.plain.ui.page.root.components.PeerListItem

@Composable
fun TabContentChat(
    navController: NavHostController,
    chatListVM: ChatListViewModel,
    paddingValues: PaddingValues
) {
    val context = LocalContext.current
    val peers by chatListVM.peers
    val isLoading by chatListVM.isLoading

    val refreshState = rememberRefreshLayoutState {
        chatListVM.loadPeers(context)
        setRefreshState(RefreshContentState.Finished)
    }

    LaunchedEffect(Unit) {
        chatListVM.loadPeers(context)
    }

    PullToRefresh(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = paddingValues.calculateTopPadding()),
        refreshLayoutState = refreshState,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                TopSpace()
            }
            
            item {
                PCard {
                    PListItem(
                        title = stringResource(R.string.local_chat),
                        subtitle = stringResource(R.string.local_chat_desc),
                        icon = R.drawable.bot,
                        modifier = Modifier.clickable {
                            navController.navigate(Routing.Chat(""))
                        }
                    )
                }
            }
            
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (peers.isNotEmpty()) {
                item {
                    VerticalSpace(dp = 16.dp)
                    Subtitle(stringResource(R.string.paired_devices))
                }
                
                item {
                    PCard {
                        peers.forEach { peer ->
                            PeerListItem(
                                title = peer.name,
                                desc = peer.ip,
                                icon = DeviceType.fromValue(peer.deviceType).getIcon(),
                                modifier = Modifier.clickable {
                                    navController.navigate(Routing.Chat("peer:${peer.id}"))
                                }
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