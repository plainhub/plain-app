package com.ismartcoding.plain.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.ViewModelStoreOwner
import com.ismartcoding.plain.rust.graphql.ContentSource
import com.ismartcoding.plain.ui.tools.ManagedDeviceViewModel
import com.ismartcoding.plain.ui.tools.ManagedDeviceStatus
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.key
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
import com.ismartcoding.plain.features.downloads.DownloadCenter
import com.ismartcoding.plain.features.downloads.isTerminalDownloadStatus
import com.ismartcoding.plain.features.shares.SharedFolderBatchTask
import com.ismartcoding.plain.features.shares.SharedFolderDownloadEngine
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.pomodoro_notification_prompt
import com.ismartcoding.plain.ui.app.MainViewModel
import com.ismartcoding.plain.ui.audio.AudioQueueViewModel
import com.ismartcoding.plain.ui.chat.ChannelViewModel
import com.ismartcoding.plain.ui.chat.PeerViewModel
import com.ismartcoding.plain.ui.chat.LocalPeerViewModel
import com.ismartcoding.plain.ui.tools.LocalManagedDeviceViewModel
import com.ismartcoding.plain.ui.dlna.DlnaCastHistoryPage
import com.ismartcoding.plain.ui.dlna.DlnaReceiverPage
import com.ismartcoding.plain.ui.downloads.components.DownloadFloatingWidget
import com.ismartcoding.plain.ui.downloads.components.DownloadListSheet
import com.ismartcoding.plain.ui.images.editor.ImageEditorListPage
import com.ismartcoding.plain.ui.images.editor.ImageEditorPage
import com.ismartcoding.plain.ui.nav.navEnterTransition
import com.ismartcoding.plain.ui.nav.navExitTransition
import com.ismartcoding.plain.ui.nav.navPopEnterTransition
import com.ismartcoding.plain.ui.nav.navPopExitTransition
import com.ismartcoding.plain.ui.nearby.NearbyPage
import com.ismartcoding.plain.ui.permissions.checkNotificationPermission
import com.ismartcoding.plain.ui.pomodoro.PomodoroPage
import com.ismartcoding.plain.ui.pomodoro.PomodoroSettingsDialog
import com.ismartcoding.plain.ui.pomodoro.PomodoroViewModel
import com.ismartcoding.plain.ui.scan.ScanHistoryPage
import com.ismartcoding.plain.ui.scan.ScanPage
import com.ismartcoding.plain.ui.search.GlobalSearchViewModel
import com.ismartcoding.plain.ui.settings.UpdateViewModel
import com.ismartcoding.plain.ui.state.launchSafe
import com.ismartcoding.plain.ui.tags.TagsViewModel
import com.ismartcoding.plain.ui.tools.SoundMeterPage

@Composable
fun MainNavGraph(
    navController: NavHostController,
    audioQueueVM: AudioQueueViewModel,
    peerVM: PeerViewModel,
    channelVM: ChannelViewModel,
    noteTagsVM: TagsViewModel,
    pomodoroVM: PomodoroViewModel,
    platformPages: (NavGraphBuilder.(NavHostController) -> Unit)?,
    batterySettings: (@Composable () -> Unit)?,
) {
    val updateVM: UpdateViewModel = viewModel { UpdateViewModel() }
    val globalSearchVM = viewModel(key = "globalSearchVM") { GlobalSearchViewModel() }
    LaunchedEffect(peerVM) { peerVM.observe() }
    LaunchedEffect(channelVM) { channelVM.observe() }
    val selectedDevice by MainViewModel.selectedDevice
    val deviceOwner = remember(selectedDevice) { object : ViewModelStoreOwner { override val viewModelStore = ViewModelStore() } }
    DisposableEffect(deviceOwner) { onDispose { deviceOwner.viewModelStore.clear() } }
    val manager = selectedDevice?.let { device ->
        viewModel(viewModelStoreOwner = deviceOwner) { ManagedDeviceViewModel(device.peerId, device.publicKey) }
    }
    val state = manager?.state?.collectAsState()?.value
    val refreshDevice: suspend () -> Boolean = manager?.let { it::refresh } ?: { true }
    val content = if (selectedDevice == null) ContentSource.Local else state?.client?.let { client -> remember(client) { ContentSource.Remote(client) } }
    val source: @Composable () -> ContentSource? = {
        when {
            state != null && (state.error != null || state.loading) -> {
                ManagedDeviceStatus(state.loading, state.error, onRefresh = refreshDevice)
                null
            }
            else -> content
        }
    }
    val pendingEntry by MainViewModel.pendingDeviceContent
    LaunchedEffect(content, state?.loading, pendingEntry) {
        val entry = pendingEntry
        if (entry != null && content != null && state?.loading != true && state?.error == null) {
            MainViewModel.pendingDeviceContent.value = null
            launchSafe { openDeviceContentEntry(navController, entry, content) }
        }
    }
    val backStackEntry by navController.currentBackStackEntryAsState()
    var showDownloads by remember { mutableStateOf(false) }
    val tasksMap = DownloadCenter.progress.collectAsState().value
    LaunchedEffect(Unit) {
        launchSafe { SharedFolderDownloadEngine.refresh() }
    }
    val shareTasks = tasksMap.values.filterIsInstance<SharedFolderBatchTask>()
    val onShareFolderPage = backStackEntry?.destination?.hasRoute<Routing.SharedFolder>() == true
    key(selectedDevice) {
        CompositionLocalProvider(
            LocalPeerViewModel provides peerVM,
            LocalManagedDeviceViewModel provides manager,
            LocalViewModelStoreOwner provides deviceOwner,
        ) {
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
                    addDeviceContentRoutes(navController, audioQueueVM, peerVM, channelVM, noteTagsVM, source)
                    addMainSettingsPages(navController, updateVM, peerVM, batterySettings)
                    composable<Routing.Home> { owner ->
                        MainRootPage(navController, owner, content, state?.loading == true, state?.error, refreshDevice, channelVM, updateVM, peerVM, audioQueueVM, globalSearchVM)
                    }
                    if (selectedDevice == null) composable<Routing.CustomFeatures> {
                        com.ismartcoding.plain.ui.tools.HomeFeaturesSelectionPage(navController)
                    }
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
                    composable<Routing.ScanHistory> { ScanHistoryPage(navController) }
                    composable<Routing.Scan> { ScanPage(navController) }
                    composable<Routing.ImageEditor> { ImageEditorListPage(navController) }
                    composable<Routing.ImageEditorDetail> { backStackEntry ->
                        val r = backStackEntry.toRoute<Routing.ImageEditorDetail>()
                        ImageEditorPage(navController, r.id)
                    }
                    composable<Routing.Nearby> { NearbyPage(navController, peerVM = peerVM) }
                    composable<Routing.DlnaReceiver> { DlnaReceiverPage(navController) }
                    composable<Routing.DlnaCastHistory> { DlnaCastHistoryPage(navController) }
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
    }
}
