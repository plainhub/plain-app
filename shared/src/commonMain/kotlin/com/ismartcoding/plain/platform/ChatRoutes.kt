package com.ismartcoding.plain.platform

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.ismartcoding.plain.ui.models.AudioQueueViewModel
import com.ismartcoding.plain.ui.models.ChannelViewModel
import com.ismartcoding.plain.chat.ChatViewModel
import com.ismartcoding.plain.ui.models.PeerViewModel
import com.ismartcoding.plain.ui.page.chat.ChatListPage
import com.ismartcoding.plain.ui.page.chat.ChatPage

@Composable
fun ChatListPageRoute(navController: NavHostController) {
    val peerVM = rememberViewModel(PeerViewModel::class) { PeerViewModel() }
    val channelVM = rememberViewModel(ChannelViewModel::class) { ChannelViewModel() }
    ChatListPage(navController, peerVM, channelVM)
}

@Composable
fun ChatPageRoute(navController: NavHostController, id: String) {
    val audioQueueVM = rememberViewModel(AudioQueueViewModel::class) { AudioQueueViewModel() }
    val chatVM = ChatViewModel
    val peerVM = rememberViewModel(PeerViewModel::class) { PeerViewModel() }
    val channelVM = rememberViewModel(ChannelViewModel::class) { ChannelViewModel() }
    ChatPage(navController, audioQueueVM, chatVM, peerVM, channelVM, id)
}
