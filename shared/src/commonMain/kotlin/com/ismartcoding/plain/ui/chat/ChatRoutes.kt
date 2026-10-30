package com.ismartcoding.plain.ui.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.plain.rust.graphql.ContentSource
import com.ismartcoding.plain.ui.audio.AudioQueueViewModel
import com.ismartcoding.plain.ui.navigation.LocalContentSource

@Composable
fun ChatListPageRoute(
    navController: NavHostController,
    owner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current),
    onTabSelected: ((Int) -> Unit)? = null,
) {
    val state = rememberChatPageState(owner)
    ChatListPage(navController, state.peers, state.channels, onTabSelected)
}

internal data class ChatPageState(val chat: ChatViewModel, val peers: PeerViewModel, val channels: ChannelViewModel, val audio: AudioQueueViewModel)

@Composable
private fun rememberChatPageState(owner: ViewModelStoreOwner, initialTargetId: String = "local"): ChatPageState {
    val source = LocalContentSource.current
    val audio = viewModel(viewModelStoreOwner = owner, key = "chat-audio") { AudioQueueViewModel(source) }
    val chat = if (source is ContentSource.Local) ChatViewModel.local
        else viewModel(viewModelStoreOwner = owner, key = "chat-content") { ChatViewModel(source, initialTargetId) }
    val peers = if (source is ContentSource.Local) viewModel(viewModelStoreOwner = owner) { PeerViewModel() }
        else viewModel(viewModelStoreOwner = owner, key = "chat-peers") { PeerViewModel(
            readPeers = { source.api.execute(com.ismartcoding.plain.graphql.Query.peerSummaries()) },
            readLatestChats = { chat.client.records.getLatestConversations() }, sourceChanges = chat.sourceChanges, api = source.api,
        ) }
    val channels = if (source is ContentSource.Local) viewModel(viewModelStoreOwner = owner) { ChannelViewModel() }
        else viewModel(viewModelStoreOwner = owner, key = "chat-channels") { ChannelViewModel(api = source.api, sourceChanges = chat.sourceChanges) }
    return ChatPageState(chat, peers, channels, audio)
}

@Composable
fun ChatPageRoute(navController: NavHostController, id: String, owner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current)) {
    val state = rememberChatPageState(owner, id)
    ChatPage(navController, state.audio, state.chat, state.peers, state.channels, id)
}

@Composable
internal fun ChatInfoPageRoute(navController: NavHostController, owner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current)) {
    val state = rememberChatPageState(owner)
    val target = state.chat.target.collectAsState().value
    if (target.type == com.ismartcoding.plain.features.chat.data.ChatTargetType.PEER) {
        PeerInfoPage(navController, state.chat.target, { state.chat.clearAllMessages() }, state.peers)
    } else ChannelInfoPage(navController, state.chat, state.peers, state.channels)
}
