package com.ismartcoding.plain.ui.page.chat

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalContext
import com.ismartcoding.lib.channel.Channel
import com.ismartcoding.plain.chat.ChatCacheManager
import com.ismartcoding.plain.enums.PickFileTag
import com.ismartcoding.plain.events.DeleteChatItemViewEvent
import com.ismartcoding.plain.events.HttpApiEvents
import com.ismartcoding.plain.events.PickFileResultEvent
import com.ismartcoding.plain.preferences.ChatInputTextPreference
import com.ismartcoding.plain.ui.models.ChatViewModel
import com.ismartcoding.plain.ui.models.PeerViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun ChatPageEffects(
    id: String,
    chatVM: ChatViewModel,
    peerVM: PeerViewModel,
    scrollState: LazyListState,
    focusManager: FocusManager,
    scope: CoroutineScope,
    onInputLoaded: (String) -> Unit,
) {
    val context = LocalContext.current
    val sharedFlow = Channel.sharedFlow

    DisposableEffect(id) {
        if (id.startsWith("peer:")) ChatCacheManager.activeChatPeerId = id.removePrefix("peer:")
        else if (id.startsWith("channel:")) ChatCacheManager.activeChatChannelId = id.removePrefix("channel:")
        onDispose {
            if (id.startsWith("peer:")) ChatCacheManager.activeChatPeerId = ""
            else if (id.startsWith("channel:")) ChatCacheManager.activeChatChannelId = ""
        }
    }

    LaunchedEffect(Unit) {
        onInputLoaded(ChatInputTextPreference.getAsync(context))
        scope.launch(Dispatchers.IO) {
            chatVM.initializeChatStateAsync(id)
            chatVM.fetchAsync(chatVM.chatState.value.toId)
        }
        peerVM.loadPeers()
    }

    LaunchedEffect(sharedFlow) {
        sharedFlow.collect { event ->
            when (event) {
                is DeleteChatItemViewEvent -> chatVM.remove(event.id)
                is HttpApiEvents.MessageCreatedEvent -> {
                    if (chatVM.chatState.value.toId == event.fromId) {
                        chatVM.addAll(event.items)
                        scope.launch { scrollState.scrollToItem(0) }
                    }
                }
                is PickFileResultEvent -> {
                    if (event.tag != PickFileTag.SEND_MESSAGE) return@collect
                    handleFileSelection(event, context, chatVM, scrollState, focusManager)
                }
            }
        }
    }
}
