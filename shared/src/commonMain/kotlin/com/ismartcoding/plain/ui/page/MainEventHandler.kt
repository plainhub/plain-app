package com.ismartcoding.plain.ui.page

import com.ismartcoding.plain.chat.RustChatStore

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.ismartcoding.plain.lib.Channel
import com.ismartcoding.plain.lib.coIO
import com.ismartcoding.plain.enums.AudioAction
import androidx.navigation.NavHostController
import androidx.navigation.NavDestination.Companion.hasRoute
import com.ismartcoding.plain.events.AudioActionEvent
import com.ismartcoding.plain.events.ConfirmDialogEvent
import com.ismartcoding.plain.events.HConfirmToAcceptLoginEvent
import com.ismartcoding.plain.events.LoadingDialogEvent
import com.ismartcoding.plain.events.HDownloadTaskDoneEvent
import com.ismartcoding.plain.ui.base.ToastEvent
import com.ismartcoding.plain.ui.models.AudioQueueViewModel
import com.ismartcoding.plain.chat.ChatViewModel
import com.ismartcoding.plain.ui.models.MainViewModel
import com.ismartcoding.plain.ui.models.PeerViewModel
import com.ismartcoding.plain.ui.models.PomodoroViewModel
import com.ismartcoding.plain.ui.nav.Routing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MainEventCollector(
    scope: CoroutineScope,
    mainVM: MainViewModel,
    chatVM: ChatViewModel,
    audioQueueVM: AudioQueueViewModel,
    pomodoroVM: PomodoroViewModel,
    peerVM: PeerViewModel,
    navController: NavHostController,
    onConfirmDialog: (ConfirmDialogEvent) -> Unit,
    onLoadingDialog: (LoadingDialogEvent) -> Unit,
    onToast: (ToastEvent) -> Unit,
    clearToast: () -> Unit,
) {
    var dismissToastJob: Job? = null
    val sharedFlow = Channel.sharedFlow

    LaunchedEffect(sharedFlow) {
        sharedFlow.collect { event ->
            when (event) {
                is ConfirmDialogEvent -> onConfirmDialog(event)
                is LoadingDialogEvent -> onLoadingDialog(event)
                is ToastEvent -> {
                    onToast(event)
                    dismissToastJob?.cancel()
                    dismissToastJob = coIO { delay(event.durationMs); clearToast() }
                }

                is AudioActionEvent -> {
                    if (event.action == AudioAction.MEDIA_ITEM_TRANSITION) {
                        scope.launch(Dispatchers.Default) { audioQueueVM.loadAsync() }
                    }
                }

                is com.ismartcoding.plain.events.HPomodoroChangedEvent -> pomodoroVM.applySnapshot(event.today)

                is HDownloadTaskDoneEvent -> {
                    scope.launch(Dispatchers.Default) {
                        val chat = RustChatStore.getById(event.downloadTask.messageId)
                        if (chat != null) {
                            chatVM.update(chat)
                        }
                    }
                }


                is HConfirmToAcceptLoginEvent -> {
                    mainVM.pendingLoginEvent.value = event
                    if (navController.currentBackStackEntry?.destination?.hasRoute<Routing.LoginRequest>() != true) {
                        navController.navigate(Routing.LoginRequest)
                    }
                }

                else -> {}
            }
        }
    }
}
