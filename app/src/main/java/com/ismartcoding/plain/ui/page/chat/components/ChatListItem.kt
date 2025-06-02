package com.ismartcoding.plain.ui.page.chat.components

import android.content.ClipData
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.R
import com.ismartcoding.plain.clipboardManager
import com.ismartcoding.plain.db.DMessageText
import com.ismartcoding.plain.db.DMessageType
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.PDropdownMenu
import com.ismartcoding.plain.ui.base.PDropdownMenuItem
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.nav.navigateChatEditText
import com.ismartcoding.plain.ui.nav.navigateChatText
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModel
import com.ismartcoding.plain.ui.models.ChatViewModel
import com.ismartcoding.plain.ui.models.VChat
import com.ismartcoding.plain.ui.models.enterSelectMode
import com.ismartcoding.plain.ui.models.select
import com.ismartcoding.plain.ui.theme.PlainTheme
import com.ismartcoding.plain.ui.theme.cardBackgroundActive

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun ChatListItem(
    navController: NavHostController,
    chatVM: ChatViewModel,
    audioPlaylistVM: AudioPlaylistViewModel,
    items: List<VChat>,
    m: VChat,
    index: Int,
    imageWidthDp: Dp,
    imageWidthPx: Int,
    focusManager: FocusManager,
    previewerState: MediaPreviewerState,
) {
    val showContextMenu = remember { mutableStateOf(false) }
    val context = LocalContext.current
    val selected = chatVM.selectedItem.value?.id == m.id || chatVM.selectedIds.contains(m.id)
    Column {
        ChatDate(items, m, index)
        Row(
            Modifier
                .background(if (selected) MaterialTheme.colorScheme.cardBackgroundActive else Color.Unspecified)
        ) {
            if (chatVM.selectMode.value) {
                HorizontalSpace(dp = 16.dp)
                Checkbox(checked = chatVM.selectedIds.contains(m.id), onCheckedChange = {
                    chatVM.select(m.id)
                })
            }
            Box(modifier = Modifier.weight(1f)) {
                Column(
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(if (chatVM.selectMode.value && !selected) 12.dp else 0.dp))
                            .combinedClickable(
                                onClick = {
                                    if (chatVM.selectMode.value) {
                                        chatVM.select(m.id)
                                    } else {
                                        focusManager.clearFocus()
                                    }
                                },
                                onLongClick = {
                                    if (chatVM.selectMode.value) {
                                        return@combinedClickable
                                    }
                                    chatVM.selectedItem.value = m
                                    showContextMenu.value = true
                                },
                                onDoubleClick = {
                                    if (m.value is DMessageText) {
                                        val content = (m.value as DMessageText).text
                                        navController.navigateChatText(content)
                                    }
                                },
                            ),
                ) {
                    ChatName(m)
                    when (m.type) {
                        DMessageType.IMAGES.value -> {
                            ChatImages(context, items, m, imageWidthDp, imageWidthPx, previewerState)
                        }

                        DMessageType.FILES.value -> {
                            ChatFiles(context, items, navController, m, audioPlaylistVM, previewerState)
                        }

                        DMessageType.TEXT.value -> {
                            ChatText(context,  chatVM,focusManager, m, onDoubleClick = {
                                val content = (m.value as DMessageText).text
                                navController.navigateChatText(content)
                            }, onLongClick = {
                                if (chatVM.selectMode.value) {
                                    return@ChatText
                                }
                                chatVM.selectedItem.value = m
                                showContextMenu.value = true
                            })
                        }
                    }
                    VerticalSpace(4.dp)
                }
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(top = 32.dp)
                            .wrapContentSize(Alignment.Center),
                ) {
                    PDropdownMenu(
                        expanded = showContextMenu.value && chatVM.selectedItem.value == m,
                        onDismissRequest = {
                            chatVM.selectedItem.value = null
                            showContextMenu.value = false
                        },
                    ) {
                        PDropdownMenuItem(
                            text = { Text(stringResource(id = R.string.select)) },
                            onClick = {
                                chatVM.enterSelectMode()
                                chatVM.select(m.id)
                                chatVM.selectedItem.value = null
                                showContextMenu.value = false
                            },
                        )
                        if (m.value is DMessageText) {
                            PDropdownMenuItem(
                                text = { Text(stringResource(id = R.string.copy_text)) },
                                onClick = {
                                    chatVM.selectedItem.value = null
                                    showContextMenu.value = false
                                    val text = (m.value as DMessageText).text
                                    val clip =
                                        ClipData.newPlainText(
                                            LocaleHelper.getString(R.string.message),
                                            text,
                                        )
                                    clipboardManager.setPrimaryClip(clip)
                                    DialogHelper.showTextCopiedMessage(text)
                                },
                            )
                            PDropdownMenuItem(
                                text = { Text(stringResource(id = R.string.edit_text)) },
                                onClick = {
                                    chatVM.selectedItem.value = null
                                    showContextMenu.value = false
                                    val content = (m.value as DMessageText).text
                                    navController.navigateChatEditText(m.id, content)
                                },
                            )
                        }
                        PDropdownMenuItem(
                            text = { Text(stringResource(id = R.string.delete)) },
                            onClick = {
                                chatVM.selectedItem.value = null
                                showContextMenu.value = false
                                chatVM.delete(context, setOf(m.id))
                            },
                        )
                    }
                }
            }
        }
        VerticalSpace(4.dp)
    }
}
