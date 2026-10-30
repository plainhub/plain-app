package com.ismartcoding.plain.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import com.ismartcoding.plain.features.chat.data.ChatTargetType
import com.ismartcoding.plain.features.downloads.DownloadCenter
import com.ismartcoding.plain.features.downloads.isTerminalDownloadStatus
import com.ismartcoding.plain.features.shares.SharedFolderBatchTask
import com.ismartcoding.plain.features.shares.SharedFolderDownloadEngine
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.pomodoro_notification_prompt
import com.ismartcoding.plain.ui.app.MainViewModel
import com.ismartcoding.plain.ui.audio.ArtistsPage
import com.ismartcoding.plain.ui.audio.AudioAllPage
import com.ismartcoding.plain.ui.audio.AudioArtistPage
import com.ismartcoding.plain.ui.audio.AudioHomePage
import com.ismartcoding.plain.ui.audio.AudioHomeViewModel
import com.ismartcoding.plain.ui.audio.AudioQueueViewModel
import com.ismartcoding.plain.ui.audio.AudioViewModel
import com.ismartcoding.plain.ui.audio.playlists.PlaylistAddItemsPage
import com.ismartcoding.plain.ui.audio.playlists.PlaylistDetailPage
import com.ismartcoding.plain.ui.casting.CastSessionPage
import com.ismartcoding.plain.ui.casting.CastViewModel
import com.ismartcoding.plain.ui.chat.ChannelInfoPage
import com.ismartcoding.plain.ui.chat.ChannelViewModel
import com.ismartcoding.plain.ui.chat.ChatListPage
import com.ismartcoding.plain.ui.chat.ChatPage
import com.ismartcoding.plain.ui.chat.ChatViewModel
import com.ismartcoding.plain.ui.chat.PeerInfoPage
import com.ismartcoding.plain.ui.chat.PeerViewModel
import com.ismartcoding.plain.ui.dlna.DlnaCastHistoryPage
import com.ismartcoding.plain.ui.dlna.DlnaReceiverPage
import com.ismartcoding.plain.ui.downloads.components.DownloadFloatingWidget
import com.ismartcoding.plain.ui.downloads.components.DownloadListSheet
import com.ismartcoding.plain.ui.feeds.FeedCatalogPage
import com.ismartcoding.plain.ui.feeds.FeedEntriesPage
import com.ismartcoding.plain.ui.feeds.FeedEntryPage
import com.ismartcoding.plain.ui.feeds.FeedEntryPagerViewModel
import com.ismartcoding.plain.ui.feeds.FeedSettingsPage
import com.ismartcoding.plain.ui.files.MediaFoldersViewModel
import com.ismartcoding.plain.ui.home.HomePage
import com.ismartcoding.plain.ui.images.ImagesPage
import com.ismartcoding.plain.ui.images.editor.ImageEditorListPage
import com.ismartcoding.plain.ui.images.editor.ImageEditorPage
import com.ismartcoding.plain.ui.nav.navEnterTransition
import com.ismartcoding.plain.ui.nav.navExitTransition
import com.ismartcoding.plain.ui.nav.navPopEnterTransition
import com.ismartcoding.plain.ui.nav.navPopExitTransition
import com.ismartcoding.plain.ui.nearby.NearbyPage
import com.ismartcoding.plain.ui.notes.NotePage
import com.ismartcoding.plain.ui.notes.NotesViewModel
import com.ismartcoding.plain.ui.permissions.checkNotificationPermission
import com.ismartcoding.plain.ui.pomodoro.PomodoroPage
import com.ismartcoding.plain.ui.pomodoro.PomodoroSettingsDialog
import com.ismartcoding.plain.ui.pomodoro.PomodoroViewModel
import com.ismartcoding.plain.ui.scan.ScanHistoryPage
import com.ismartcoding.plain.ui.scan.ScanPage
import com.ismartcoding.plain.ui.search.GlobalSearchPage
import com.ismartcoding.plain.ui.search.GlobalSearchViewModel
import com.ismartcoding.plain.ui.settings.UpdateViewModel
import com.ismartcoding.plain.ui.state.launchSafe
import com.ismartcoding.plain.ui.tags.TagsViewModel
import com.ismartcoding.plain.ui.tools.SoundMeterPage
import com.ismartcoding.plain.ui.tools.ToolsPage
import com.ismartcoding.plain.ui.videos.VideosPage

@Composable
fun MainNavGraph(
    navController: NavHostController,
    audioQueueVM: AudioQueueViewModel,
    peerVM: PeerViewModel,
    channelVM: ChannelViewModel,
    feedTagsVM: TagsViewModel,
    feedEntryPagerVM: FeedEntryPagerViewModel,
    noteTagsVM: TagsViewModel,
    pomodoroVM: PomodoroViewModel,
    platformPages: (NavGraphBuilder.(NavHostController) -> Unit)?,
    batterySettings: (@Composable () -> Unit)?,
) {
    val updateVM: UpdateViewModel = viewModel { UpdateViewModel() }
    val audioVM = viewModel(key = "audioVM") { AudioViewModel() }
    val audioTagsVM = viewModel(key = "audioTagsVM") { TagsViewModel() }
    val audioFoldersVM = viewModel(key = "audioFoldersVM") { MediaFoldersViewModel() }
    val audioCastVM = viewModel(key = "audioCastVM") { CastViewModel() }
    val audioHomeVM = viewModel(key = "audioHomeVM") { AudioHomeViewModel() }
    val globalSearchVM = viewModel(key = "globalSearchVM") { GlobalSearchViewModel() }
    val backStackEntry by navController.currentBackStackEntryAsState()
    var showDownloads by remember { mutableStateOf(false) }
    val tasksMap = DownloadCenter.progress.collectAsState().value
    LaunchedEffect(Unit) {
        launchSafe { SharedFolderDownloadEngine.refresh() }
    }
    val shareTasks = tasksMap.values.filterIsInstance<SharedFolderBatchTask>()
    val onShareFolderPage = backStackEntry?.destination?.hasRoute<Routing.SharedFolder>() == true
    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            modifier = Modifier.background(MaterialTheme.colorScheme.surface),
            navController = navController,
            startDestination = Routing.Home,
            enterTransition = { navEnterTransition(PRESENTED_ROUTES) },
            exitTransition = { navExitTransition(PRESENTED_ROUTES) },
            popEnterTransition = { navPopEnterTransition(PRESENTED_ROUTES) },
            popExitTransition = { navPopExitTransition(PRESENTED_ROUTES) },
        ) {
            platformPages?.invoke(this, navController)
            addMainFilePages(navController, audioQueueVM)
            addManagedDevicePages(navController, audioQueueVM)
            addMainSettingsPages(navController, updateVM, peerVM, batterySettings)
            composable<Routing.Home> {
                val selectedTab by MainViewModel.currentRootTab
                val onTabSelected: (Int) -> Unit = { MainViewModel.currentRootTab.value = it }
                when (selectedTab) {
                    0 -> HomePage(navController, updateVM, onTabSelected = onTabSelected)
                    1 -> ChatListPage(navController, peerVM = peerVM, channelVM = channelVM, onTabSelected = onTabSelected)
                    2 -> ToolsPage(navController, onTabSelected = onTabSelected)
                    3 -> GlobalSearchPage(navController, audioQueueVM, globalSearchVM, onTabSelected = onTabSelected)
                }
            }
            composable<Routing.Images> { ImagesPage(navController, initialLoadGate = rememberNavLoadGate()) }
            composable<Routing.Videos> { VideosPage(navController, initialLoadGate = rememberNavLoadGate()) }
            composable<Routing.Audio> {
                AudioHomePage(navController, audioQueueVM, audioVM, audioTagsVM, audioFoldersVM, audioCastVM, audioHomeVM)
            }
            composable<Routing.AudioAll> {
                AudioAllPage(navController, audioQueueVM, audioVM, audioTagsVM, audioFoldersVM, audioCastVM)
            }
            composable<Routing.Artists> { ArtistsPage(navController, audioVM, audioHomeVM) }
            composable<Routing.ArtistDetail> { backStackEntry ->
                val r = backStackEntry.toRoute<Routing.ArtistDetail>()
                AudioArtistPage(navController, r.name, audioQueueVM, audioVM, audioTagsVM, audioFoldersVM, audioCastVM)
            }
            composable<Routing.PlaylistDetail> { backStackEntry ->
                val r = backStackEntry.toRoute<Routing.PlaylistDetail>()
                PlaylistDetailPage(navController, r.id, audioQueueVM, audioVM, audioTagsVM, audioFoldersVM, audioCastVM)
            }
            composable<Routing.PlaylistAddItems> { backStackEntry ->
                val r = backStackEntry.toRoute<Routing.PlaylistAddItems>()
                PlaylistAddItemsPage(navController, r.id)
            }
            addAppsPages(navController)
            addNotesPages(navController) { NotesNavigationState(NotesViewModel.local, noteTagsVM) }
            composable<Routing.SoundMeter> { SoundMeterPage(navController) }

            composable<Routing.PomodoroTimer> {
                PomodoroPage(
                    navController = navController,
                    pomodoroVM = pomodoroVM,
                    settingsDialog = { settings, onSettingsChange, onDismiss ->
                        PomodoroSettingsDialog(settings = settings, onSettingsChange = onSettingsChange, onDismiss = onDismiss)
                    },
                    onCheckNotificationPermission = { onGranted ->
                        checkNotificationPermission(Res.string.pomodoro_notification_prompt) { onGranted() }
                    },
                )
            }
            composable<Routing.Chat> { backStackEntry ->
                val r = backStackEntry.toRoute<Routing.Chat>()
                ChatPage(navController, audioQueueVM = audioQueueVM, chatVM = ChatViewModel.local, peerVM = peerVM, channelVM = channelVM, r.id)
            }
            composable<Routing.ChatInfo> {
                val chatVM = ChatViewModel.local
                val chatTarget = chatVM.target.collectAsState()
                if (chatTarget.value.type == ChatTargetType.PEER) {
                    PeerInfoPage(navController, chatVM.target, { chatVM.clearAllMessages() }, peerVM)
                } else {
                    ChannelInfoPage(navController, chatVM, peerVM, channelVM)
                }
            }
            composable<Routing.ScanHistory> { ScanHistoryPage(navController) }
            composable<Routing.Scan> { ScanPage(navController) }
            composable<Routing.FeedSettings> { FeedSettingsPage(navController) }
            composable<Routing.FeedCatalog> { FeedCatalogPage(navController) }
            composable<Routing.FeedEntries> { backStackEntry ->
                val r = backStackEntry.toRoute<Routing.FeedEntries>()
                FeedEntriesPage(navController, r.feedId, tagsVM = feedTagsVM, pagerVM = feedEntryPagerVM)
            }
            composable<Routing.FeedEntry> { backStackEntry ->
                val r = backStackEntry.toRoute<Routing.FeedEntry>()
                FeedEntryPage(navController, r.id, tagsVM = feedTagsVM, pagerVM = feedEntryPagerVM)
            }
            composable<Routing.ImageEditor> { ImageEditorListPage(navController) }
            composable<Routing.ImageEditorDetail> { backStackEntry ->
                val r = backStackEntry.toRoute<Routing.ImageEditorDetail>()
                ImageEditorPage(navController, r.id)
            }
            composable<Routing.Nearby> { NearbyPage(navController, peerVM = peerVM) }
            composable<Routing.DlnaReceiver> { DlnaReceiverPage(navController) }
            composable<Routing.DlnaCastHistory> { DlnaCastHistoryPage(navController) }
            composable<Routing.CastSession> { CastSessionPage(navController) }
            addMainRequestPages(navController, channelVM)
        }

        if (!onShareFolderPage && shareTasks.any { !it.status.isTerminalDownloadStatus() }) {
            DownloadFloatingWidget(
                tasks = shareTasks,
                onClick = { showDownloads = true },
            )
        }
    }

    if (showDownloads) {
        DownloadListSheet(onDismiss = { showDownloads = false })
    }
}
