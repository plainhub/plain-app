package com.ismartcoding.plain.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.NavHostController
import com.ismartcoding.plain.rust.graphql.ContentSource
import com.ismartcoding.plain.ui.app.MainViewModel
import com.ismartcoding.plain.ui.audio.AudioQueueViewModel
import com.ismartcoding.plain.ui.chat.ChatListPageRoute
import com.ismartcoding.plain.ui.chat.PeerViewModel
import com.ismartcoding.plain.ui.components.ActionButtonSettings
import com.ismartcoding.plain.ui.search.GlobalSearchPage
import com.ismartcoding.plain.ui.search.GlobalSearchViewModel
import com.ismartcoding.plain.ui.tools.ManagedDeviceStatus
import com.ismartcoding.plain.ui.tools.ToolsPage

@Composable
internal fun MainRootPage(
    navController: NavHostController,
    owner: ViewModelStoreOwner,
    source: ContentSource?,
    loading: Boolean,
    error: String?,
    refreshDevice: suspend () -> Boolean,
    channelVM: com.ismartcoding.plain.ui.chat.ChannelViewModel,
    updateVM: com.ismartcoding.plain.ui.settings.UpdateViewModel,
    localPeerVM: PeerViewModel,
    audioQueueVM: AudioQueueViewModel,
    globalSearchVM: GlobalSearchViewModel,
) {
    val requestedTab by MainViewModel.currentRootTab
    val items = mainNavItems()
    val selectedTab = requestedTab.takeIf { tab -> items.any { it.index == tab } } ?: 1
    val onTabSelected: (Int) -> Unit = { MainViewModel.currentRootTab.intValue = it }
    if (selectedTab == 3) {
        CompositionLocalProvider(LocalContentSource provides ContentSource.Local) {
            GlobalSearchPage(navController, audioQueueVM, globalSearchVM, onTabSelected = onTabSelected)
        }
    } else if (loading || error != null || source == null) {
        MainNavScaffold(selectedIndex = selectedTab, onTabSelected = onTabSelected, topBar = {
            DeviceTopAppBar(navController) {
                ActionButtonSettings { navController.navigate(Routing.Settings) }
            }
        }) { padding ->
            Box(Modifier.padding(padding)) {
                ManagedDeviceStatus(loading, error, tools = selectedTab == 2, onRefresh = refreshDevice)
            }
        }
    } else {
        CompositionLocalProvider(LocalContentSource provides source) {
            when (selectedTab) {
                0 -> com.ismartcoding.plain.ui.home.HomePage(navController, updateVM, onTabSelected)
                1 -> if (source is ContentSource.Local) com.ismartcoding.plain.ui.chat.ChatListPage(navController, localPeerVM, channelVM, onTabSelected, observeUpdates = false)
                    else ChatListPageRoute(navController, owner, onTabSelected)
                2 -> ToolsPage(navController, onTabSelected)
            }
        }
    }
}
