package com.ismartcoding.plain.ui.chat.components

import com.ismartcoding.plain.ui.state.launchSafe

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.features.chat.download.DownloadTask
import com.ismartcoding.plain.features.chat.models.MessageFile
import com.ismartcoding.plain.features.files.TextFileType
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.lib.coMain
import com.ismartcoding.plain.lib.extensions.isAudioFast
import com.ismartcoding.plain.lib.extensions.isImageFast
import com.ismartcoding.plain.lib.extensions.isPdfFile
import com.ismartcoding.plain.lib.extensions.isTextFile
import com.ismartcoding.plain.lib.extensions.isVideoFast
import com.ismartcoding.plain.lib.extensions.isZipFile
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.ui.i18n.LocaleHelper
import com.ismartcoding.plain.platform.files.fileToUriString
import com.ismartcoding.plain.platform.media.playAudioWithNotificationCheck
import com.ismartcoding.plain.platform.files.saveFileToDownloads
import com.ismartcoding.plain.ui.base.PDropdownMenu
import com.ismartcoding.plain.ui.base.PDropdownMenuItem
import com.ismartcoding.plain.ui.components.SaveToSheet
import com.ismartcoding.plain.ui.media.viewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.media.viewer.previewer.TransformItemState
import com.ismartcoding.plain.ui.components.DialogHelper
import com.ismartcoding.plain.ui.media.MediaPreviewData
import com.ismartcoding.plain.ui.chat.VChat
import com.ismartcoding.plain.ui.navigation.navigateOtherFile
import com.ismartcoding.plain.ui.navigation.navigatePdf
import com.ismartcoding.plain.ui.navigation.navigateTextFile
import com.ismartcoding.plain.ui.navigation.navigateZipFile
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ChatFileItemContent(
    items: List<VChat>,
    navController: NavHostController,
    item: MessageFile,
    itemState: TransformItemState,
    previewerState: MediaPreviewerState,
    fileName: String,
    path: String,
    previewPath: String,
    isActive: Boolean,
    downloadProgress: Float,
    downloadTask: DownloadTask?,
    isCurrentlyPlaying: Boolean,
    showContextMenu: MutableState<Boolean>,
    index: Int,
) {
    val source = com.ismartcoding.plain.ui.navigation.LocalContentSource.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    var showSaveSheet by remember { mutableStateOf(false) }

    if (showSaveSheet) {
        SaveToSheet(
            title = fileName,
            source = source,
            onDismiss = { showSaveSheet = false },
            onDownloads = {
                showSaveSheet = false
                coMain {
                    val result = withIO { if (source is com.ismartcoding.plain.rust.graphql.ContentSource.Remote) source.files.copyToDirectory(path, source.info.downloadsDir, fileName) else saveFileToDownloads(path, fileName) }
                    if (result.isNotEmpty()) {
                        DialogHelper.showConfirmDialog("", LocaleHelper.getStringFAsync(Res.string.file_save_to, result))
                    } else {
                        DialogHelper.showErrorMessage(result)
                    }
                }
            },
            onDirectory = { dir ->
                showSaveSheet = false
                coMain {
                    val result = withIO { source.files.copyToDirectory(path, dir, fileName) }
                    if (result.isNotEmpty()) {
                        DialogHelper.showConfirmDialog("", LocaleHelper.getStringFAsync(Res.string.file_save_to, result))
                    } else {
                        DialogHelper.showErrorMessage(result)
                    }
                }
            },
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (isActive || source is com.ismartcoding.plain.rust.graphql.ContentSource.Remote && item.isRemoteFile()) return@combinedClickable
                    if (fileName.isImageFast() || fileName.isVideoFast()) {
                        coMain {
                            keyboardController?.hide()
                            withIO { MediaPreviewData.setDataAsync(itemState, items.reversed(), item, source) }
                            previewerState.openTransform(
                                index = MediaPreviewData.items.indexOfFirst { it.id == item.id },
                                itemState = itemState,
                            )
                        }
                    } else if (fileName.isAudioFast()) {
                        playAudioWithNotificationCheck(path, source)
                    } else if (fileName.isTextFile()) {
                        navController.navigateTextFile(path, fileName, mediaId = "", type = TextFileType.CHAT)
                    } else if (fileName.isPdfFile()) {
                        scope.launchSafe { navController.navigatePdf(fileToUriString(source.fileForOpen(path, fileName)), fileName) }
                    } else if (fileName.isZipFile()) {
                        navController.navigateZipFile(path, fileName)
                    } else {
                        navController.navigateOtherFile(path, fileName)
                    }
                },
                onLongClick = {
                    showContextMenu.value = true
                },
            ),
    ) {
        PDropdownMenu(
            expanded = showContextMenu.value,
            onDismissRequest = { showContextMenu.value = false },
        ) {
            PDropdownMenuItem(
                text = { Text(stringResource(Res.string.save_as)) },
                onClick = {
                    showContextMenu.value = false
                    showSaveSheet = true
                },
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = if (index == 0) 16.dp else 6.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ChatFileInfo(
                modifier = Modifier.weight(1f),
                fileName = fileName,
                size = item.size,
                durationMs = item.durationMs,
                summary = item.summary,
                isCurrentlyPlaying = isCurrentlyPlaying,
            )

            ChatFileThumbnail(
                fileName = fileName,
                previewPath = previewPath,
                item = item,
                itemState = itemState,
                previewerState = previewerState,
                isActive = isActive,
                downloadProgress = downloadProgress,
                downloadTask = downloadTask,
            )
        }
    }
}
