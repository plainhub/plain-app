package com.ismartcoding.plain.ui.page.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.R
import com.ismartcoding.plain.chat.ChatCacheManager
import com.ismartcoding.plain.db.DChatChannel
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.fastscroll.LazyColumnScrollbar
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshLayoutState
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModel
import com.ismartcoding.plain.ui.models.ChatState
import com.ismartcoding.plain.ui.models.ChatType
import com.ismartcoding.plain.ui.models.ChatViewModel
import com.ismartcoding.plain.ui.models.VChat
import com.ismartcoding.plain.ui.models.forwardMessage
import com.ismartcoding.plain.ui.models.forwardMessageToLocal
import com.ismartcoding.plain.ui.models.showBottomActions
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.PeerViewModel
import com.ismartcoding.plain.ui.page.chat.components.ChatInput
import com.ismartcoding.plain.ui.page.chat.components.ChatListItem
import com.ismartcoding.plain.ui.page.chat.components.ForwardTarget
import com.ismartcoding.plain.ui.page.chat.components.ForwardTargetDialog
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun ChatPageContent(
    navController: NavHostController,
    chatVM: ChatViewModel,
    audioPlaylistVM: AudioPlaylistViewModel,
    chatState: ChatState,
    itemsState: List<VChat>,
    channelStatus: String?,
    paddingValues: PaddingValues,
    refreshState: RefreshLayoutState,
    scrollState: LazyListState,
    focusManager: FocusManager,
    previewerState: MediaPreviewerState,
    imageWidthDp: Dp,
    imageWidthPx: Int,
    inputValue: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    peerVM: PeerViewModel,
) {
    var showForwardDialog by remember { mutableStateOf(false) }
    var messageToForward by remember { mutableStateOf<VChat?>(null) }
    Column(modifier = Modifier.fillMaxSize().padding(top = paddingValues.calculateTopPadding())) {
        PullToRefresh(modifier = Modifier.weight(1f), refreshLayoutState = refreshState) {
            LazyColumnScrollbar(state = scrollState) {
                LazyColumn(state = scrollState, reverseLayout = true, verticalArrangement = Arrangement.Top) {
                    item(key = "bottomSpace") { VerticalSpace(dp = paddingValues.calculateBottomPadding()) }
                    itemsIndexed(itemsState, key = { _, a -> a.id }) { index, m ->
                        ChatListItem(
                            navController = navController, chatVM = chatVM, audioPlaylistVM,
                            itemsState, m = m,
                            peer = (if (chatState.chatType == ChatType.PEER) ChatCacheManager.peerMap[chatState.toId] else null)
                                ?: ChatCacheManager.peerMap[m.fromId],
                            index = index, imageWidthDp = imageWidthDp, imageWidthPx = imageWidthPx,
                            focusManager = focusManager, previewerState = previewerState,
                            onForward = { message -> messageToForward = message; showForwardDialog = true },
                        )
                    }
                }
            }
        }
        val peer = if (chatState.chatType == ChatType.PEER) ChatCacheManager.peerMap[chatState.toId] else null
        val channelInactive = channelStatus == DChatChannel.STATUS_LEFT || channelStatus == DChatChannel.STATUS_KICKED
        if (channelInactive) {
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(if (channelStatus == DChatChannel.STATUS_KICKED) R.string.channel_kicked_notice else R.string.channel_left_notice),
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, textAlign = TextAlign.Center,
                )
            }
        } else if (!chatVM.showBottomActions() && (peer == null || peer.status == "paired")) {
            ChatInput(value = inputValue, hint = stringResource(R.string.chat_input_hint), onValueChange = onInputChange, onSend = onSend)
        }
    }

    if (showForwardDialog) {
        ForwardTargetDialog(peerVM = peerVM, onDismiss = { showForwardDialog = false; messageToForward = null },
            onTargetSelected = { target ->
                messageToForward?.let { message ->
                    when (target) {
                        is ForwardTarget.Local -> chatVM.forwardMessageToLocal(message.id) { DialogHelper.showSuccess(R.string.sent) }
                        is ForwardTarget.Peer -> chatVM.forwardMessage(message.id, target.peer) { DialogHelper.showSuccess(R.string.sent) }
                    }
                }
            })
    }
}
