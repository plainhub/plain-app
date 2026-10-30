package com.ismartcoding.plain.ui.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.ismartcoding.plain.features.chat.ShareSendClient
import com.ismartcoding.plain.features.chat.data.ChatTarget
import com.ismartcoding.plain.lib.TimeHelper
import com.ismartcoding.plain.ui.chat.components.FolderShareSheet
import com.ismartcoding.plain.ui.chat.components.ForwardTargetDialog
import kotlinx.coroutines.launch

@Composable
internal fun ChatPageDialogs(
    showFolderShare: Boolean,
    target: ChatTarget,
    onDismissFolder: () -> Unit,
    forwardMessage: VChat?,
    onDismissForward: () -> Unit,
    onForward: (VChat, List<ChatTarget>) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val source = com.ismartcoding.plain.ui.navigation.LocalContentSource.current
    if (showFolderShare) {
        FolderShareSheet(
            onDismiss = onDismissFolder,
            onConfirm = { path, name, expiry ->
                onDismissFolder()
                scope.launch {
                    source.chatSender.sendFolderShare(listOf(target), path, name, expiry.expiresAt(TimeHelper.now()))
                }
            },
        )
    }
    if (forwardMessage != null) {
        ForwardTargetDialog(
            onDismiss = onDismissForward,
            onTargetsSelected = { targets ->
                onForward(forwardMessage, targets)
                onDismissForward()
            },
        )
    }
}
