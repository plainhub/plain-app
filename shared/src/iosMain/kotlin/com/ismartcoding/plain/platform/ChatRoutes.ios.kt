package com.ismartcoding.plain.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import com.ismartcoding.plain.ui.models.ChannelViewModel
import com.ismartcoding.plain.ui.models.ChatViewModel
import com.ismartcoding.plain.ui.models.IosAudioPlaylistViewModel
import com.ismartcoding.plain.ui.models.MainViewModel
import com.ismartcoding.plain.ui.models.PeerViewModel
import com.ismartcoding.plain.ui.page.chat.ChatListPage
import com.ismartcoding.plain.ui.page.chat.ChatPage

@Composable
actual fun ChatListPageRoute(navController: NavHostController) {
    val mainVM = remember { MainViewModel() }
    val peerVM = remember { PeerViewModel() }
    val channelVM = remember { ChannelViewModel() }
    ChatListPage(navController, mainVM, peerVM, channelVM)
}

@Composable
actual fun ChatPageRoute(navController: NavHostController, id: String) {
    val audioPlaylistVM = remember { IosAudioPlaylistViewModel() }
    val chatVM = remember { ChatViewModel() }
    val peerVM = remember { PeerViewModel() }
    val channelVM = remember { ChannelViewModel() }
    ChatPage(navController, audioPlaylistVM, chatVM, peerVM, channelVM, id)
}
