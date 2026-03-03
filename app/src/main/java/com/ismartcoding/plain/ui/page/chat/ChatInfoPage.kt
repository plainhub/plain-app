package com.ismartcoding.plain.ui.page.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavHostController
import com.ismartcoding.plain.ui.models.ChannelViewModel
import com.ismartcoding.plain.ui.models.ChatType
import com.ismartcoding.plain.ui.models.ChatViewModel
import com.ismartcoding.plain.ui.models.PeerViewModel

/**
 * Dispatcher that routes to [PeerChatInfoPage] or [ChannelChatInfoPage]
 * depending on the current chat type held in [chatVM].
 */
@Composable
fun ChatInfoPage(
    navController: NavHostController,
    chatVM: ChatViewModel,
    peerVM: PeerViewModel,
    channelVM: ChannelViewModel,
) {
    val chatState = chatVM.chatState.collectAsState()
    if (chatState.value.chatType == ChatType.PEER) {
        PeerChatInfoPage(navController, chatVM, peerVM)
    } else {
        ChannelChatInfoPage(navController, chatVM, peerVM, channelVM)
    }
}
