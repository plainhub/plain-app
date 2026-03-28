package com.ismartcoding.plain.ui.page

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.ismartcoding.lib.channel.Channel
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.lib.helpers.JsonHelper
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.ChatItemDataUpdate
import com.ismartcoding.plain.db.DMessageContent
import com.ismartcoding.plain.db.DMessageText
import com.ismartcoding.plain.db.DMessageType
import com.ismartcoding.plain.enums.AudioAction
import com.ismartcoding.plain.events.AudioActionEvent
import com.ismartcoding.plain.events.ChannelUpdatedEvent
import com.ismartcoding.plain.events.ConfirmDialogEvent
import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.FetchLinkPreviewsEvent
import com.ismartcoding.plain.events.HttpApiEvents
import com.ismartcoding.plain.events.LoadingDialogEvent
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.chat.ChatDbHelper
import com.ismartcoding.plain.features.LinkPreviewHelper
import com.ismartcoding.plain.ui.base.ToastEvent
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModel
import com.ismartcoding.plain.ui.models.ChatViewModel
import com.ismartcoding.plain.ui.models.PeerViewModel
import com.ismartcoding.plain.ui.models.PomodoroViewModel
import com.ismartcoding.plain.web.models.toModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MainEventCollector(
    scope: CoroutineScope,
    context: Context,
    chatVM: ChatViewModel,
    audioPlaylistVM: AudioPlaylistViewModel,
    pomodoroVM: PomodoroViewModel,
    peerVM: PeerViewModel,
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
                    dismissToastJob = coIO { delay(event.duration); clearToast() }
                }

                is AudioActionEvent -> {
                    if (event.action == AudioAction.MEDIA_ITEM_TRANSITION) {
                        scope.launch(Dispatchers.IO) { audioPlaylistVM.loadAsync(context) }
                    }
                }

                is FetchLinkPreviewsEvent -> {
                    scope.launch(Dispatchers.IO) {
                        val data = event.chat.content.value as DMessageText
                        val urls = LinkPreviewHelper.extractUrls(data.text)
                        if (urls.isNotEmpty()) {
                            val links = ChatDbHelper.fetchLinkPreviewsAsync(context, urls).filter { !it.hasError }
                            if (links.isNotEmpty()) {
                                val updatedMessageText = DMessageText(data.text, links)
                                event.chat.content = DMessageContent(DMessageType.TEXT.value, updatedMessageText)
                                AppDatabase.instance.chatDao().updateData(
                                    ChatItemDataUpdate(event.chat.id, event.chat.content)
                                )
                                chatVM.update(event.chat)
                                val m = event.chat.toModel()
                                m.data = m.getContentData()
                                sendEvent(WebSocketEvent(EventType.MESSAGE_UPDATED, JsonHelper.jsonEncode(listOf(m))))
                            }
                        }
                    }
                }

                is HttpApiEvents.PomodoroStartEvent -> {
                    pomodoroVM.timeLeft.intValue = event.timeLeft
                    pomodoroVM.startSession()
                }
                is HttpApiEvents.PomodoroPauseEvent -> pomodoroVM.pauseSession()
                is HttpApiEvents.PomodoroStopEvent -> pomodoroVM.resetTimer()

                is HttpApiEvents.DownloadTaskDoneEvent -> {
                    scope.launch(Dispatchers.IO) {
                        val chat = AppDatabase.instance.chatDao().getById(event.downloadTask.messageId)
                        if (chat != null) {
                            chatVM.update(chat)
                            val m = chat.toModel()
                            m.data = m.getContentData()
                            sendEvent(WebSocketEvent(EventType.MESSAGE_UPDATED, JsonHelper.jsonEncode(listOf(m))))
                        }
                    }
                }

                is ChannelUpdatedEvent -> peerVM.loadPeers()
                else -> {}
            }
        }
    }
}
