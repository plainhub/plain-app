package com.ismartcoding.plain.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.NavHostController
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.chat
import com.ismartcoding.plain.rust.graphql.ContentSource
import com.ismartcoding.plain.ui.app.MainViewModel
import com.ismartcoding.plain.ui.audio.AudioQueueViewModel
import com.ismartcoding.plain.ui.base.NoDataColumn
import com.ismartcoding.plain.ui.chat.ChatListPageRoute
import com.ismartcoding.plain.ui.chat.PeerViewModel
import com.ismartcoding.plain.ui.components.ActionButtonSettings
import com.ismartcoding.plain.ui.search.GlobalSearchPage
import com.ismartcoding.plain.ui.search.GlobalSearchViewModel
import com.ismartcoding.plain.ui.tools.ManagedError
import com.ismartcoding.plain.ui.tools.ToolsPage
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun MainRootPage(
    navController: NavHostController,
    owner: ViewModelStoreOwner,
    source: ContentSource?,
    loading: Boolean,
    error: String?,
    channelVM: com.ismartcoding.plain.ui.chat.ChannelViewModel,
    updateVM: com.ismartcoding.plain.ui.settings.UpdateViewModel,
    localPeerVM: PeerViewModel,
    audioQueueVM: AudioQueueViewModel,
    globalSearchVM: GlobalSearchViewModel,
) {
    LaunchedEffect(localPeerVM) { localPeerVM.observe() }
    val requestedTab by MainViewModel.currentRootTab
    val items = mainNavItems()
    val selectedTab = requestedTab.takeIf { tab -> items.any { it.index == tab } } ?: 1
    val onTabSelected: (Int) -> Unit = { MainViewModel.currentRootTab.intValue = it }
    if (selectedTab == 3) {
        CompositionLocalProvider(LocalContentSource provides ContentSource.Local) {
            GlobalSearchPage(navController, audioQueueVM, globalSearchVM, onTabSelected = onTabSelected)
        }
    } else if (loading || error != null || source == null) {
        val navigation = LocalDeviceNavigation.current!!
        MainNavScaffold(selectedIndex = selectedTab, onTabSelected = onTabSelected, topBar = {
            DeviceTopAppBar(fallbackTitle = stringResource(Res.string.chat)) {
                ActionButtonSettings(navigation.onSettings)
            }
        }) { padding ->
            Box(Modifier.padding(padding)) {
                if (error != null) ManagedError(error) else NoDataColumn(loading = true)
            }
        }
    } else {
        CompositionLocalProvider(LocalContentSource provides source) {
            when (selectedTab) {
                0 -> com.ismartcoding.plain.ui.home.HomePage(navController, updateVM, onTabSelected)
                1 -> if (source is ContentSource.Local) com.ismartcoding.plain.ui.chat.ChatListPage(navController, localPeerVM, channelVM, onTabSelected, observePeers = false)
                    else ChatListPageRoute(navController, owner, onTabSelected)
                2 -> ToolsPage(navController, onTabSelected)
            }
        }
    }
}
