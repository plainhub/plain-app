package com.ismartcoding.plain.ui.chat

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ismartcoding.plain.events.PickFileResultEvent
import com.ismartcoding.plain.features.chat.download.DownloadQueue
import com.ismartcoding.plain.features.chat.data.ChatTargetType
import com.ismartcoding.plain.features.files.PickFileTag
import com.ismartcoding.plain.lib.Channel
import com.ismartcoding.plain.ui.state.launchSafe

internal val LocalChatDownloads = androidx.compose.runtime.staticCompositionLocalOf<com.ismartcoding.plain.features.chat.download.ChatDownloadClient> { DownloadQueue }

@Composable
internal fun ChatPageEffects(
    chatVM: ChatViewModel, peerVM: PeerViewModel, channelVM: ChannelViewModel,
    id: String, targetType: ChatTargetType, focusManager: FocusManager, scrollState: LazyListState,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(chatVM, lifecycleOwner, id) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) chatVM.visibleConversationId.value = id
            else if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                chatVM.visibleConversationId.compareAndSet(id, null)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            chatVM.visibleConversationId.compareAndSet(id, null)
        }
    }
    LaunchedEffect(chatVM, id) {
        launchSafe {
            chatVM.initializeTargetAsync(id)
            chatVM.fetchAsync(chatVM.target.value.toId)
        }
    }
    LaunchedEffect(peerVM) { peerVM.observe() }
    LaunchedEffect(channelVM, targetType) {
        if (targetType == ChatTargetType.CHANNEL) channelVM.observe()
    }
    LaunchedEffect(chatVM.source) {
        if (chatVM.source is com.ismartcoding.plain.rust.graphql.ContentSource.Local) launchSafe { chatVM.downloads.refresh() }
    }
    LaunchedEffect(chatVM, peerVM, focusManager) {
        Channel.sharedFlow.collect { event ->
            if (event is PickFileResultEvent && event.tag == PickFileTag.SEND_MESSAGE) {
                handleChatFileSelection(event, chatVM, peerVM, focusManager)
            }
        }
    }
    LaunchedEffect(chatVM, scrollState) {
        chatVM.scrollToLatest.collect { previousTopId ->
            scrollToLatest(chatVM, scrollState, previousTopId)
        }
    }
}
