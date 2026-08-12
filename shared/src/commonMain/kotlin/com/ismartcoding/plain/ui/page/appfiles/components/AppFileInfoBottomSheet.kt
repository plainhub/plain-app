package com.ismartcoding.plain.ui.page.appfiles.components

import com.ismartcoding.plain.i18n.*

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.db.DMessageContent
import com.ismartcoding.plain.db.DMessageFile
import com.ismartcoding.plain.db.DMessageFiles
import com.ismartcoding.plain.db.DMessageImages
import com.ismartcoding.plain.db.MessageType
import com.ismartcoding.plain.extensions.resolveAppFileRealPath
import com.ismartcoding.plain.lib.extensions.formatBytes
import com.ismartcoding.plain.lib.extensions.isImageFast
import com.ismartcoding.plain.lib.extensions.isVideoFast
import com.ismartcoding.plain.platform.formatDateTime
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.CopyIconButton
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.models.ChatViewModel
import com.ismartcoding.plain.ui.models.VAppFile
import com.ismartcoding.plain.ui.nav.Routing
import com.ismartcoding.plain.ui.page.chat.components.ForwardTargetDialog
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppFileInfoBottomSheet(
    file: VAppFile,
    chatVM: ChatViewModel,
    navController: NavHostController,
    onDismiss: () -> Unit,
) {
    var showForwardDialog by remember { mutableStateOf(false) }

    if (showForwardDialog) {
        ForwardTargetDialog(
            onDismiss = { showForwardDialog = false },
            onTargetSelected = { target ->
                val dFile = DMessageFile(
                    uri = file.appFile.getFidUri(),
                    size = file.appFile.size,
                    fileName = file.fileName,
                )
                val isImageVideo = file.fileName.isImageFast() ||
                        file.fileName.isVideoFast() ||
                        file.appFile.mimeType.startsWith("image/") ||
                        file.appFile.mimeType.startsWith("video/")
                val content = if (isImageVideo) {
                    DMessageContent(MessageType.IMAGES, DMessageImages(listOf(dFile)))
                } else {
                    DMessageContent(MessageType.FILES, DMessageFiles(listOf(dFile)))
                }
                chatVM.setPendingForwardContent(content)
                showForwardDialog = false
                onDismiss()
                navController.navigate(Routing.Chat(target.toId))
            }
        )
    }

    PModalBottomSheet(onDismissRequest = { onDismiss() }) {
        LazyColumn {
            item { VerticalSpace(32.dp) }
            item {
                AppFileInfoActionButtons(
                    file = file,
                    onShowForwardDialog = { showForwardDialog = true },
                    onDismiss = onDismiss,
                )
                VerticalSpace(dp = 24.dp)
                PCard {
                    PListItem(title = file.appFile.realPath.resolveAppFileRealPath(), action = {
                        CopyIconButton(text = file.appFile.realPath.resolveAppFileRealPath(), clipLabel = stringResource(Res.string.file_path))
                    })
                }
                VerticalSpace(dp = 16.dp)
                PCard {
                    PListItem(title = stringResource(Res.string.file_size), value = file.appFile.size.formatBytes())
                    PListItem(title = stringResource(Res.string.type), value = file.appFile.mimeType)
                    PListItem(title = stringResource(Res.string.file_name), value = file.fileName)
                    PListItem(title = stringResource(Res.string.created_at), value = file.appFile.createdAt.formatDateTime())
                    PListItem(title = stringResource(Res.string.updated_at), value = file.appFile.updatedAt.formatDateTime())
                }
            }
            item { BottomSpace() }
        }
    }
}