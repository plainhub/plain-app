package com.ismartcoding.plain.ui.page.chat.components

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavHostController
import com.ismartcoding.lib.extensions.isAudioFast
import com.ismartcoding.lib.extensions.isImageFast
import com.ismartcoding.lib.extensions.isPdfFile
import com.ismartcoding.lib.extensions.isTextFile
import com.ismartcoding.lib.extensions.isVideoFast
import com.ismartcoding.lib.helpers.CoroutinesHelper.coMain
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.chat.download.DownloadTask
import com.ismartcoding.plain.audio.DPlaylistAudio
import com.ismartcoding.plain.db.DMessageFile
import com.ismartcoding.plain.enums.TextFileType
import com.ismartcoding.plain.audio.AudioPlayer
import com.ismartcoding.plain.features.Permissions
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.helpers.FileHelper
import com.ismartcoding.plain.ui.base.PDropdownMenu
import com.ismartcoding.plain.ui.base.PDropdownMenuItem
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.TransformItemState
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.MediaPreviewData
import com.ismartcoding.plain.ui.models.VChat
import com.ismartcoding.plain.ui.nav.navigateOtherFile
import com.ismartcoding.plain.ui.nav.navigatePdf
import com.ismartcoding.plain.ui.nav.navigateTextFile
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ChatFileItemContent(
    context: Context,
    items: List<VChat>,
    navController: NavHostController,
    item: DMessageFile,
    itemState: TransformItemState,
    previewerState: MediaPreviewerState,
    fileName: String,
    path: String,
    previewPath: String,
    isDownloading: Boolean,
    downloadProgress: Float,
    downloadTask: DownloadTask?,
    isCurrentlyPlaying: Boolean,
    showContextMenu: MutableState<Boolean>,
    index: Int,
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (isDownloading) return@combinedClickable
                    if (fileName.isImageFast() || fileName.isVideoFast()) {
                        coMain {
                            keyboardController?.hide()
                            withIO { MediaPreviewData.setDataAsync(context, itemState, items.reversed(), item) }
                            previewerState.openTransform(
                                index = MediaPreviewData.items.indexOfFirst { it.id == item.id },
                                itemState = itemState,
                            )
                        }
                    } else if (fileName.isAudioFast()) {
                        Permissions.checkNotification(context, R.string.audio_notification_prompt) {
                            AudioPlayer.play(context, DPlaylistAudio.fromPath(context, path))
                        }
                    } else if (fileName.isTextFile()) {
                        navController.navigateTextFile(path, fileName, mediaId = "", type = TextFileType.CHAT)
                    } else if (fileName.isPdfFile()) {
                        navController.navigatePdf(File(path).toUri(), fileName)
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
                text = { Text(stringResource(R.string.save)) },
                onClick = {
                    showContextMenu.value = false
                    coMain {
                        val result = withIO { FileHelper.copyFileToDownloads(path, fileName) }
                        if (result.isNotEmpty()) {
                            DialogHelper.showConfirmDialog("", LocaleHelper.getStringF(R.string.file_save_to, "path", result))
                        } else {
                            DialogHelper.showErrorMessage(result)
                        }
                    }
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
                duration = item.duration,
                summary = item.summary,
                isCurrentlyPlaying = isCurrentlyPlaying,
            )

            ChatFileThumbnail(
                context = context,
                fileName = fileName,
                previewPath = previewPath,
                item = item,
                itemState = itemState,
                previewerState = previewerState,
                isDownloading = isDownloading,
                downloadProgress = downloadProgress,
                downloadTask = downloadTask,
            )
        }
    }
}
