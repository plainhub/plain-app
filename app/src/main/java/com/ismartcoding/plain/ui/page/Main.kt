package com.ismartcoding.plain.ui.page

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.ismartcoding.lib.extensions.isGestureInteractionMode
import com.ismartcoding.lib.isQPlus
import com.ismartcoding.plain.enums.DarkTheme
import com.ismartcoding.plain.events.ConfirmDialogEvent
import com.ismartcoding.plain.events.LoadingDialogEvent
import com.ismartcoding.plain.preferences.LocalDarkTheme
import com.ismartcoding.plain.ui.base.ToastEvent
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModel
import com.ismartcoding.plain.ui.models.ChannelViewModel
import com.ismartcoding.plain.ui.models.ChatViewModel
import com.ismartcoding.plain.ui.models.MainViewModel
import com.ismartcoding.plain.ui.models.NotesViewModel
import com.ismartcoding.plain.ui.models.PeerViewModel
import com.ismartcoding.plain.ui.models.PomodoroViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.nav.Routing
import com.ismartcoding.plain.ui.theme.AppTheme
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
    notesVM: NotesViewModel = viewModel(key = "notesVM"),
    feedTagsVM: TagsViewModel = viewModel(key = "feedTagsVM"),
    noteTagsVM: TagsViewModel = viewModel(key = "noteTagsVM"),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    navControllerState.value = navController
    val useDarkTheme = DarkTheme.isDarkTheme(LocalDarkTheme.current)
    val view = LocalView.current

    var confirmDialogEvent by remember { mutableStateOf<ConfirmDialogEvent?>(null) }
    var loadingDialogEvent by remember { mutableStateOf<LoadingDialogEvent?>(null) }
    var toastState by remember { mutableStateOf<ToastEvent?>(null) }

    val activity = context as? Activity
    LaunchedEffect(loadingDialogEvent) {
        if (loadingDialogEvent != null) activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    LaunchedEffect(Unit) {
        onLaunched()
        scope.launch(Dispatchers.IO) { pomodoroVM.loadAsync(context) }
    }

    MainEventCollector(
        scope, context, chatVM, audioPlaylistVM, pomodoroVM, peerVM,
        onConfirmDialog = { confirmDialogEvent = it },
        onLoadingDialog = { loadingDialogEvent = if (it.show) it else null },
        onToast = { toastState = it },
        clearToast = { toastState = null },
    )

    AppTheme(useDarkTheme = useDarkTheme) {
        DisposableEffect(useDarkTheme) {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = !useDarkTheme
                isAppearanceLightNavigationBars = !useDarkTheme
            }
            if (isQPlus() && context.isGestureInteractionMode()) {
                window.isNavigationBarContrastEnforced = false
            }
            onDispose { }
        }

        Box(modifier = Modifier.background(MaterialTheme.colorScheme.backgroundNormal)) {
            MainNavGraph(navController, mainVM, audioPlaylistVM, chatVM, peerVM, channelVM, notesVM, feedTagsVM, noteTagsVM, pomodoroVM)

            MainDialogs(loadingDialogEvent, confirmDialogEvent, { confirmDialogEvent = null }, toastState, { toastState = null })
        }
    }
}

