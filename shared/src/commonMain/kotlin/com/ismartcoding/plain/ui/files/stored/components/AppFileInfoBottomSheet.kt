package com.ismartcoding.plain.ui.files.stored.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.features.chat.ShareSendClient
import com.ismartcoding.plain.features.chat.models.MessageContent
import com.ismartcoding.plain.features.chat.models.MessageFile
import com.ismartcoding.plain.features.chat.models.MessageFiles
import com.ismartcoding.plain.features.chat.models.MessageImages
import com.ismartcoding.plain.features.chat.models.MessageType
import com.ismartcoding.plain.features.files.stored.AppFileStore
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.lib.extensions.formatBytes
import com.ismartcoding.plain.lib.extensions.isImageFast
import com.ismartcoding.plain.lib.extensions.isVideoFast
import com.ismartcoding.plain.platform.system.formatDateTime
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.CopyIconButton
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.components.DialogHelper
import com.ismartcoding.plain.ui.files.stored.VAppFile
import com.ismartcoding.plain.ui.chat.components.ForwardTargetDialog
import com.ismartcoding.plain.ui.theme.PlainTheme
import com.ismartcoding.plain.ui.state.launchSafe
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppFileInfoBottomSheet(
    file: VAppFile,
    onDismiss: () -> Unit,
) {
    val source = com.ismartcoding.plain.ui.navigation.LocalContentSource.current
    var showForwardDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    if (showForwardDialog) {
        ForwardTargetDialog(
            onDismiss = { showForwardDialog = false },
            onTargetsSelected = { targets ->
                showForwardDialog = false
                scope.launchSafe {
                    val dFile = MessageFile(uri = AppFileStore.toFidUri(file.appFile), size = file.appFile.size, fileName = file.fileName)
                    val isMedia = file.fileName.isImageFast() || file.fileName.isVideoFast() || file.appFile.mimeType.startsWith("image/") || file.appFile.mimeType.startsWith("video/")
                    val content = if (isMedia) MessageContent(MessageType.IMAGES, MessageImages(listOf(dFile)))
                        else MessageContent(MessageType.FILES, MessageFiles(listOf(dFile)))
                    source.chatSender.sendContent(targets, content)
                    DialogHelper.showSuccess(Res.string.sent)
                    onDismiss()
                }
            }
        )
    }

    PModalBottomSheet(onDismissRequest = { onDismiss() }) {
        LazyColumn {
            item { VerticalSpace(16.dp) }
            item {
                AppFileInfoActionButtons(
                    file = file,
                    onShowForwardDialog = { showForwardDialog = true },
                    onDismiss = onDismiss,
                )
                VerticalSpace(dp = 24.dp)
                PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
                    PListItem(title = file.appFile.realPath, action = {
                        CopyIconButton(text = file.appFile.realPath, clipLabel = stringResource(Res.string.file_path))
                    })
                }
                VerticalSpace(dp = 16.dp)
                PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
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
