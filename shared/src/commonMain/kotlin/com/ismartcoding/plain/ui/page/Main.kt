package com.ismartcoding.plain.ui.page
import com.ismartcoding.plain.preferences.*

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.ismartcoding.plain.platform.applySystemBarAppearanceForDarkTheme
import com.ismartcoding.plain.platform.isDebugBuild
import com.ismartcoding.plain.platform.keepScreenOn
import com.ismartcoding.plain.enums.DarkTheme
import com.ismartcoding.plain.events.ConfirmDialogEvent
import com.ismartcoding.plain.events.LoadingDialogEvent
import com.ismartcoding.plain.events.ShowPermissionWizardEvent
import com.ismartcoding.plain.httpserver.HttpServerManager
import com.ismartcoding.plain.lib.Channel
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.preferences.LocalDarkTheme
import com.ismartcoding.plain.ui.base.DebugCornerBadge
import com.ismartcoding.plain.ui.base.ToastEvent
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModel
import com.ismartcoding.plain.ui.models.ChannelViewModel
import com.ismartcoding.plain.ui.models.ChatViewModel
import com.ismartcoding.plain.ui.models.FeedEntryPagerViewModel
import com.ismartcoding.plain.ui.models.MainViewModel
import com.ismartcoding.plain.ui.models.NotesViewModel
import com.ismartcoding.plain.ui.models.PeerViewModel
import com.ismartcoding.plain.ui.models.PomodoroViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.nav.Routing
import com.ismartcoding.plain.ui.page.dlna.DlnaReceiverOverlay
import com.ismartcoding.plain.ui.page.audioplayer.AudioPlayerPage
import com.ismartcoding.plain.ui.page.home.ServiceOnboardingWizard
import com.ismartcoding.plain.ui.theme.backgroundNormal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun Main(
    navControllerState: MutableState<NavHostController?>,
    onLaunched: () -> Unit,
    mainVM: MainViewModel,
    audioPlaylistVM: AudioPlaylistViewModel,
    pomodoroVM: PomodoroViewModel,
    chatVM: ChatViewModel,
    peerVM: PeerViewModel,
    channelVM: ChannelViewModel,
    feedTagsVM: TagsViewModel = viewModel(key = "feedTagsVM") { TagsViewModel() },
    feedEntryPagerVM: FeedEntryPagerViewModel = viewModel(key = "feedEntryPagerVM") { FeedEntryPagerViewModel() },
    noteTagsVM: TagsViewModel = viewModel(key = "noteTagsVM") { TagsViewModel() },
) {
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    navControllerState.value = navController
    val useDarkTheme = DarkTheme.isDarkTheme(LocalDarkTheme.current)

    var confirmDialogEvent by remember { mutableStateOf<ConfirmDialogEvent?>(null) }
    var loadingDialogEvent by remember { mutableStateOf<LoadingDialogEvent?>(null) }
    var toastState by remember { mutableStateOf<ToastEvent?>(null) }
    var showPermissionWizard by remember { mutableStateOf(false) }

    LaunchedEffect(loadingDialogEvent) {
        keepScreenOn(loadingDialogEvent != null)
    }

    LaunchedEffect(Channel.sharedFlow) {
        Channel.sharedFlow.collect { event ->
            if (event is ShowPermissionWizardEvent) {
                showPermissionWizard = true
            }
        }
    }

    LaunchedEffect(Unit) {
        onLaunched()
        scope.launch(Dispatchers.Default) { pomodoroVM.loadAsync() }
    }

    MainEventCollector(
        scope, mainVM, chatVM, audioPlaylistVM, pomodoroVM, peerVM, navController,
        onConfirmDialog = { confirmDialogEvent = it },
        onLoadingDialog = { loadingDialogEvent = if (it.show) it else null },
        onToast = { toastState = it },
        clearToast = { toastState = null },
    )

    // Single source of truth for the system bar appearance: the theme Compose
    // is actually rendering. SideEffect re-applies after EVERY recomposition,
    // so any competing writer (enableEdgeToEdge's config-change re-apply,
    // insets controller state, etc.) is corrected on the next frame instead of
    // leaving inverted status bar icons until restart.
    SideEffect {
        applySystemBarAppearanceForDarkTheme(useDarkTheme)
    }

    Box(modifier = Modifier.background(MaterialTheme.colorScheme.backgroundNormal)) {
        MainNavGraph(navController, mainVM, audioPlaylistVM, chatVM, peerVM, channelVM, feedTagsVM, feedEntryPagerVM, noteTagsVM, pomodoroVM)

        // Global fullscreen audio player: hosted above all pages so playback
        // keeps working across tabs, with the page beneath visible during the
        // slide-up transition.
        val audioPlayerVisible by TempData.audioPlayerVisible.collectAsState()
        if (audioPlayerVisible) {
            AudioPlayerPage(audioPlaylistVM) { TempData.audioPlayerVisible.value = false }
        }

        DlnaReceiverOverlay()
        MainDialogs(loadingDialogEvent, confirmDialogEvent, { confirmDialogEvent = null }, toastState, { toastState = null })
        if (showPermissionWizard) {
            ServiceOnboardingWizard(
                // Permissions are resolved (or explicitly skipped) at the
                // final step — dispatch the start unconditionally.
                onStartService = {
                    showPermissionWizard = false
                    HttpServerManager.dispatchStart()
                },
                onClose = { showPermissionWizard = false },
            )
        }
        if (isDebugBuild()) {
            DebugCornerBadge(Modifier.align(Alignment.TopStart))
        }
    }
}
