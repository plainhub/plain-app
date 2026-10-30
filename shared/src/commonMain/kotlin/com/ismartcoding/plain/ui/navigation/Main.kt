package com.ismartcoding.plain.ui.navigation

import com.ismartcoding.plain.ui.state.launchSafe

import com.ismartcoding.plain.ui.media.extensions.collectAsStateValue

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.key
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.rememberNavController
import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.ui.platform.system.applySystemBarAppearanceForDarkTheme
import com.ismartcoding.plain.platform.system.isDebugBuild
import com.ismartcoding.plain.ui.theme.DarkTheme
import com.ismartcoding.plain.ui.base.DebugCornerBadge
import com.ismartcoding.plain.ui.audio.AudioQueueViewModel
import com.ismartcoding.plain.ui.chat.ChannelViewModel
import com.ismartcoding.plain.ui.app.MainViewModel
import com.ismartcoding.plain.ui.chat.PeerViewModel
import com.ismartcoding.plain.ui.pomodoro.PomodoroViewModel
import com.ismartcoding.plain.ui.tags.TagsViewModel
import com.ismartcoding.plain.ui.dlna.DlnaReceiverOverlay
import com.ismartcoding.plain.ui.audio.player.AudioPlayerPage
import com.ismartcoding.plain.ui.media.ShortcutMediaPreviewer
import com.ismartcoding.plain.ui.theme.backgroundNormal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun Main(
    navControllerState: MutableState<NavHostController?>,
    onLaunched: () -> Unit,
    audioQueueVM: AudioQueueViewModel,
    pomodoroVM: PomodoroViewModel,
    peerVM: PeerViewModel,
    channelVM: ChannelViewModel,
    noteTagsVM: TagsViewModel = viewModel(key = "noteTagsVM") { TagsViewModel() },
    platformPages: (NavGraphBuilder.(NavHostController) -> Unit)? = null,
    batterySettings: (@Composable () -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()
    val selectedDevice by MainViewModel.selectedDevice
    val navController = key(selectedDevice) { rememberNavController() }
    navControllerState.value = navController
    val useDarkTheme = DarkTheme.isDarkTheme(Prefs.darkTheme.collectAsStateValue())

    val audioOutput by com.ismartcoding.plain.features.audio.AudioCommands.output.collectAsState()
    val playbackSource = audioOutput.source
    val playingQueue = if (playbackSource is com.ismartcoding.plain.rust.graphql.ContentSource.Local) audioQueueVM
        else {
            val owner = androidx.compose.runtime.remember(playbackSource) {
                object : androidx.lifecycle.ViewModelStoreOwner {
                    override val viewModelStore = androidx.lifecycle.ViewModelStore()
                }
            }
            androidx.compose.runtime.DisposableEffect(owner) { onDispose { owner.viewModelStore.clear() } }
            viewModel(viewModelStoreOwner = owner) { AudioQueueViewModel(playbackSource) }
        }
    LaunchedEffect(playingQueue) { scope.launchSafe { playingQueue.loadAsync() } }
    LaunchedEffect(playingQueue.selectedPath.value) {
        if (playingQueue.selectedPath.value.isEmpty() && audioOutput.track == null) playingQueue.audioPlayerVisible.value = false
    }

    LaunchedEffect(Unit) {
        onLaunched()
        scope.launch(Dispatchers.Default) { pomodoroVM.loadAsync() }
    }

    LaunchedEffect(Unit) {
        if (!Prefs.onboardingCompleted.value) {
            // singleTop: this effect re-runs on activity recreation while the
            // onboarding is still showing and the pref is not yet written
            navController.navigate(Routing.Onboarding) {
                launchSingleTop = true
            }
        }
    }

    SideEffect {
        applySystemBarAppearanceForDarkTheme(useDarkTheme)
    }

    Box(modifier = Modifier.background(MaterialTheme.colorScheme.backgroundNormal)) {
        MainNavGraph(navController, audioQueueVM, peerVM, channelVM, noteTagsVM, pomodoroVM, platformPages, batterySettings)
        MainRequestNavigation(navController)

        // Global fullscreen audio player: hosted above all pages so playback
        // keeps working across tabs, with the page beneath visible during the
        // slide-up transition.
        val audioPlayerVisible by playingQueue.audioPlayerVisible.collectAsState()
        if (audioPlayerVisible) {
            androidx.compose.runtime.CompositionLocalProvider(LocalContentSource provides playbackSource) {
                AudioPlayerPage(playingQueue) { playingQueue.audioPlayerVisible.value = false }
            }
        }

        // Home-screen shortcut media preview: overlay instead of a route so the
        // app UI stays composed beneath and closing the preview reveals it.
        val shortcutMediaPath by MainViewModel.shortcutMediaPath
        if (shortcutMediaPath != null) {
            ShortcutMediaPreviewer(shortcutMediaPath!!) { MainViewModel.shortcutMediaPath.value = null }
        }

        DlnaReceiverOverlay()
        MainDialogs()
        if (isDebugBuild()) {
            DebugCornerBadge(Modifier.align(Alignment.TopStart))
        }
    }
}
