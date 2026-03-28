package com.ismartcoding.plain.ui.page.chat.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.db.DMessageStatusData
import com.ismartcoding.plain.db.DMessageText
import com.ismartcoding.plain.db.DMessageType
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModel
import com.ismartcoding.plain.ui.models.ChatType
import com.ismartcoding.plain.ui.models.ChatViewModel
import com.ismartcoding.plain.ui.models.VChat
import com.ismartcoding.plain.ui.models.resendToMembers
import com.ismartcoding.plain.ui.models.retryMessage
import com.ismartcoding.plain.ui.models.select
import com.ismartcoding.plain.ui.nav.navigateChatText
import com.ismartcoding.plain.ui.theme.cardBackgroundActive

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun ChatListItem(
    navController: NavHostController,
    chatVM: ChatViewModel,
    audioPlaylistVM: AudioPlaylistViewModel,
    items: List<VChat>,
    m: VChat,
    peer: DPeer?,
    index: Int,
    imageWidthDp: Dp,
    imageWidthPx: Int,
    focusManager: FocusManager,
    previewerState: MediaPreviewerState,
    onForward: (VChat) -> Unit = {},
) {
    val showContextMenu = remember { mutableStateOf(false) }
    val showDeliveryDialog = remember { mutableStateOf<DMessageStatusData?>(null) }
    val context = LocalContext.current
    val selected = chatVM.selectedItem.value?.id == m.id || chatVM.selectedIds.contains(m.id)
    Column {
        ChatDate(items, m, index)
        Row(Modifier.background(if (selected) MaterialTheme.colorScheme.cardBackgroundActive else Color.Unspecified)) {
            if (chatVM.selectMode.value) {
                HorizontalSpace(dp = 16.dp)
                Checkbox(checked = chatVM.selectedIds.contains(m.id), onCheckedChange = { chatVM.select(m.id) })
            }
            Box(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(if (chatVM.selectMode.value && !selected) 12.dp else 0.dp))
                        .combinedClickable(
                            onClick = { if (chatVM.selectMode.value) chatVM.select(m.id) else focusManager.clearFocus() },
                            onLongClick = {
                                if (chatVM.selectMode.value) return@combinedClickable
                                chatVM.selectedItem.value = m
                                showContextMenu.value = true
                            },
                            onDoubleClick = {
                                if (m.value is DMessageText) navController.navigateChatText((m.value as DMessageText).text)
                            },
                        ),
                ) {
                    ChatName(m = m, isPeerChat = peer != null, isLocal = chatVM.chatState.value.chatType == ChatType.LOCAL,
                        onRetry = if (chatVM.chatState.value.chatType != ChatType.LOCAL) { { chatVM.retryMessage(m.id) } } else null,
                        onShowDeliveryDetails = if (chatVM.chatState.value.chatType != ChatType.LOCAL) { { statusData -> showDeliveryDialog.value = statusData } } else null)
                    when (m.type) {
                        DMessageType.IMAGES.value -> ChatImages(context, items, m, peer, imageWidthDp, imageWidthPx, previewerState, chatVM)
                        DMessageType.FILES.value -> ChatFiles(context, items, navController, m, peer, audioPlaylistVM, previewerState)
                        DMessageType.TEXT.value -> ChatText(context, chatVM, focusManager, m, onDoubleClick = {
                            navController.navigateChatText((m.value as DMessageText).text)
                        }, onLongClick = {
                            if (chatVM.selectMode.value) return@ChatText
                            chatVM.selectedItem.value = m
                            showContextMenu.value = true
                        })
                    }
                    VerticalSpace(4.dp)
                }
                Box(modifier = Modifier.fillMaxSize().padding(top = 32.dp).wrapContentSize(Alignment.Center)) {
                    ChatListItemContextMenu(navController, chatVM, m, showContextMenu, onForward)
                }
            }
        }
        VerticalSpace(4.dp)

        showDeliveryDialog.value?.let { statusData ->
            if (peer != null) {
                PeerDeliveryStatusDialog(statusData = statusData, onRetry = { chatVM.retryMessage(m.id) }, onDismiss = { showDeliveryDialog.value = null })
            } else {
                ChannelDeliveryStatusDialog(statusData = statusData, onResend = { peerIds -> chatVM.resendToMembers(m.id, peerIds) }, onDismiss = { showDeliveryDialog.value = null })
            }
        }
    }
}
