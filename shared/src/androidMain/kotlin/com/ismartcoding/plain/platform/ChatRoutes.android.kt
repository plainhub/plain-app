package com.ismartcoding.plain.platform

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModel
import com.ismartcoding.plain.ui.models.ChannelViewModel
import com.ismartcoding.plain.ui.models.ChatViewModel
import com.ismartcoding.plain.ui.models.MainViewModel
import com.ismartcoding.plain.ui.models.PeerViewModel
import com.ismartcoding.plain.ui.page.chat.ChatListPage
import com.ismartcoding.plain.ui.page.chat.ChatPage

@Composable
actual fun ChatListPageRoute(navController: NavHostController) {
    val mainVM: MainViewModel = viewModel()
    val peerVM: PeerViewModel = viewModel()
    val channelVM: ChannelViewModel = viewModel()
    ChatListPage(navController, mainVM, peerVM, channelVM)
}

@Composable
actual fun ChatPageRoute(navController: NavHostController, id: String) {
    val audioPlaylistVM: AudioPlaylistViewModel = viewModel()
    val chatVM: ChatViewModel = viewModel()
    val peerVM: PeerViewModel = viewModel()
    val channelVM: ChannelViewModel = viewModel()
    ChatPage(navController, audioPlaylistVM, chatVM, peerVM, channelVM, id)
}
