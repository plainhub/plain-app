package com.ismartcoding.plain.ui.files.stored.components

import com.ismartcoding.plain.features.media.MediaClient

import androidx.navigation.NavHostController
import com.ismartcoding.plain.features.chat.models.MessageFile
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.lib.coMain
import com.ismartcoding.plain.lib.extensions.isAudioFast
import com.ismartcoding.plain.lib.extensions.isImageFast
import com.ismartcoding.plain.lib.extensions.isPdfFile
import com.ismartcoding.plain.lib.extensions.isTextFile
import com.ismartcoding.plain.lib.extensions.isVideoFast
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.platform.files.fileToUriString
import com.ismartcoding.plain.platform.media.playAudioWithNotificationCheck
import com.ismartcoding.plain.ui.media.viewer.PreviewItem
import com.ismartcoding.plain.ui.media.viewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.media.viewer.previewer.TransformItemState
import com.ismartcoding.plain.ui.components.DialogHelper
import com.ismartcoding.plain.ui.audio.AudioQueueViewModel
import com.ismartcoding.plain.ui.media.MediaPreviewData
import com.ismartcoding.plain.ui.files.stored.VAppFile
import com.ismartcoding.plain.ui.navigation.navigateOtherFile
import com.ismartcoding.plain.ui.navigation.navigatePdf
import com.ismartcoding.plain.ui.navigation.navigateTextFile

fun openAppFile(
    files: List<VAppFile>,
    file: VAppFile,
    navController: NavHostController,
    previewerState: MediaPreviewerState,
    itemState: TransformItemState,
    audioQueueVM: AudioQueueViewModel,
    source: com.ismartcoding.plain.rust.graphql.ContentSource = com.ismartcoding.plain.rust.graphql.ContentSource.Local,
) {
    if (source is com.ismartcoding.plain.rust.graphql.ContentSource.Remote) {
        val entries = files.map { com.ismartcoding.plain.models.File(name = it.fileName, path = it.appFile.realPath, size = it.appFile.size, createdAt = it.appFile.createdAt, updatedAt = it.appFile.updatedAt, isDir = false, childCount = 0) }
        com.ismartcoding.plain.ui.files.components.openFile(entries, entries.first { it.path == file.appFile.realPath }, navController, previewerState, itemState, audioQueueVM, source)
        return
    }
    val path = file.appFile.realPath
    val fileName = file.fileName

    when {
        fileName.isImageFast() || fileName.isVideoFast() -> {
            coMain {
                val previewItems = withIO {
                    files.filter { it.fileName.isImageFast() || it.fileName.isVideoFast() }.map {
                        val p = it.appFile.realPath
                        PreviewItem(
                            it.appFile.id,
                            p,
                            it.appFile.size,
                            data = MessageFile(uri = p, size = it.appFile.size, fileName = it.fileName),
                        )
                    }
                }
                withIO {
                    MediaPreviewData.setDataAsync(
                        itemState,
                        previewItems,
                        PreviewItem(file.appFile.id, path, file.appFile.size, data = MessageFile(uri = path, size = file.appFile.size, fileName = fileName)),
                    )
                }
                previewerState.openTransform(
                    index = MediaPreviewData.items.indexOfFirst { it.id == file.appFile.id },
                    itemState = itemState,
                )
            }
        }

        fileName.isAudioFast() -> {
            coMain {
                try {
                    val audio = withIO { MediaClient.audioInfo(path) }
                    audioQueueVM.playSingleAsync(audio)
                    playAudioWithNotificationCheck(path)
                } catch (ex: Exception) {
                    DialogHelper.showMessage(Res.string.audio_play_error)
                }
            }
        }

        fileName.isTextFile() -> {
            navController.navigateTextFile(path, fileName)
        }

        fileName.isPdfFile() -> {
            try {
                navController.navigatePdf(fileToUriString(path))
            } catch (ex: Exception) {
                DialogHelper.showMessage(Res.string.pdf_open_error)
            }
        }

        else -> navController.navigateOtherFile(path, fileName)
    }
}
