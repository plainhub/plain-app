package com.ismartcoding.plain.platform

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModel
import com.ismartcoding.plain.ui.models.ChannelViewModel
import com.ismartcoding.plain.ui.models.ChatViewModel
import com.ismartcoding.plain.ui.models.MainViewModel
import com.ismartcoding.plain.ui.models.PeerViewModel
import com.ismartcoding.plain.ui.page.chat.ChatListPage
import com.ismartcoding.plain.ui.page.chat.ChatPage

@Composable
fun ChatListPageRoute(navController: NavHostController) {
    val mainVM = rememberViewModel(MainViewModel::class) { MainViewModel() }
    val peerVM = rememberViewModel(PeerViewModel::class) { PeerViewModel() }
    val channelVM = rememberViewModel(ChannelViewModel::class) { ChannelViewModel() }
    ChatListPage(navController, mainVM, peerVM, channelVM)
}

@Composable
fun ChatPageRoute(navController: NavHostController, id: String) {
    val audioPlaylistVM = rememberViewModel(AudioPlaylistViewModel::class) { AudioPlaylistViewModel() }
    val chatVM = rememberViewModel(ChatViewModel::class) { ChatViewModel() }
    val peerVM = rememberViewModel(PeerViewModel::class) { PeerViewModel() }
    val channelVM = rememberViewModel(ChannelViewModel::class) { ChannelViewModel() }
    ChatPage(navController, audioPlaylistVM, chatVM, peerVM, channelVM, id)
}
