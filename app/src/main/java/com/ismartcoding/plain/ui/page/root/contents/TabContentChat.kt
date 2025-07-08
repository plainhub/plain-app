package com.ismartcoding.plain.ui.page.root.contents

import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavHostController
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.R
import com.ismartcoding.plain.enums.DeviceType
import com.ismartcoding.plain.features.nearby.NearbyDiscoverManager
import com.ismartcoding.plain.preferences.NearbyDiscoverablePreference
import com.ismartcoding.plain.preferences.dataFlow
import com.ismartcoding.plain.preferences.dataStore
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.Subtitle
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.extensions.collectAsStateValue
import com.ismartcoding.plain.ui.models.ChatListViewModel
import com.ismartcoding.plain.ui.nav.Routing
import com.ismartcoding.plain.ui.page.root.components.PeerListItem
import com.ismartcoding.plain.ui.page.root.components.RootTabType
import com.ismartcoding.plain.ui.theme.PlainTheme
import com.ismartcoding.plain.web.ChatApiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun TabContentChat(
    navController: NavHostController,
    chatListVM: ChatListViewModel,
    paddingValues: PaddingValues,
    pagerState: PagerState
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val pairedPeers = chatListVM.pairedPeers
    val unpairedPeers = chatListVM.unpairedPeers

    val scope = rememberCoroutineScope()

    // Track app lifecycle and screen state
    var isAppInForeground by remember { mutableStateOf(true) }
    var isScreenOn by remember { mutableStateOf(true) }
    var isPageVisible by remember { mutableStateOf(true) }

    val isDiscoverable = remember {
        context.dataStore.dataFlow.map {
            NearbyDiscoverablePreference.get(it)
        }
    }.collectAsStateValue(initial = NearbyDiscoverablePreference.default)

    val refreshState = rememberRefreshLayoutState {
        chatListVM.loadPeers()
        setRefreshState(RefreshContentState.Finished)
    }

    // Monitor app lifecycle
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    isAppInForeground = true
                    isPageVisible = true
                }

                Lifecycle.Event.ON_PAUSE -> {
                    isAppInForeground = false
                    isPageVisible = false
                }

                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(context) {
        val screenReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: android.content.Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_SCREEN_ON -> isScreenOn = true
                    Intent.ACTION_SCREEN_OFF -> isScreenOn = false
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        context.registerReceiver(screenReceiver, filter)

        onDispose {
            context.unregisterReceiver(screenReceiver)
        }
    }

    LaunchedEffect(Unit) {
        chatListVM.loadPeers()
    }

    // Device discovery timer with dynamic interval
    LaunchedEffect(Unit) {
        while (isActive) {
            scope.launch(Dispatchers.IO) {
                pairedPeers.forEach { peer ->
                    val key = ChatApiManager.peerKeyCache[peer.id]
                    if (key != null) {
                        try {
                            NearbyDiscoverManager.discoverSpecificDevice(peer.id, key)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            LogCat.e("Error discovering device ${peer.id}: ${e.message}")
                        }
                    }
                }
            }

            val isCurrentTab = pagerState.currentPage == RootTabType.CHAT.value
            // Dynamic interval based on app state
            val interval = if (isCurrentTab && isPageVisible && isAppInForeground && isScreenOn) {
                5000L  // 5 seconds when user is on Chat tab, foreground, and screen on
            } else {
                15000L // 15 seconds otherwise
            }

            LogCat.d("Discovery interval: ${interval}ms (currentTab: $isCurrentTab [visible: $isPageVisible, foreground: $isAppInForeground, screenOn: $isScreenOn)")
            delay(interval)
        }
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
                        title = stringResource(R.string.make_discoverable),
                        subtitle = stringResource(R.string.make_discoverable_desc),
                        icon = R.drawable.wifi,
                        action = {
                            Switch(
                                checked = isDiscoverable,
                                onCheckedChange = { enabled ->
                                    chatListVM.updateDiscoverable(context, enabled)
                                }
                            )
                        }
                    )
                }
            }

            item {
                VerticalSpace(dp = 16.dp)
            }

            item {
                PeerListItem(
                    title = stringResource(R.string.local_chat),
                    desc = stringResource(R.string.local_chat_desc),
                    icon = R.drawable.bot,
                    latestChat = chatListVM.getLatestChat("local"),
                    modifier = PlainTheme
                        .getCardModifier()
                        .clickable {
                            navController.navigate(Routing.Chat(""))
                        }
                )
            }

            if (pairedPeers.isNotEmpty()) {
                item {
                    VerticalSpace(dp = 16.dp)
                    Subtitle(stringResource(R.string.paired_devices))
                }

                items(
                    items = pairedPeers,
                ) { peer ->
                    PeerListItem(
                        title = peer.name,
                        desc = peer.ip,
                        icon = DeviceType.fromValue(peer.deviceType).getIcon(),
                        online = chatListVM.getPeerOnlineStatus(peer.id),
                        latestChat = chatListVM.getLatestChat(peer.id),
                        modifier = PlainTheme
                            .getCardModifier()
                            .clickable {
                                navController.navigate(Routing.Chat("peer:${peer.id}"))
                            }
                    )
                    VerticalSpace(8.dp)
                }
            }

            if (unpairedPeers.isNotEmpty()) {
                item {
                    VerticalSpace(dp = 8.dp)
                    Subtitle(stringResource(R.string.unpaired_devices))
                }

                items(
                    items = unpairedPeers,
                ) { peer ->
                    PeerListItem(
                        title = peer.name,
                        desc = peer.ip,
                        icon = DeviceType.fromValue(peer.deviceType).getIcon(),
                        online = chatListVM.getPeerOnlineStatus(peer.id),
                        latestChat = chatListVM.getLatestChat(peer.id),
                        modifier = PlainTheme
                            .getCardModifier()
                            .clickable {
                                navController.navigate(Routing.Chat("peer:${peer.id}"))
                            }
                    )
                    VerticalSpace(8.dp)
                }
            }

            item {
                BottomSpace(paddingValues)
            }
        }
    }
} 