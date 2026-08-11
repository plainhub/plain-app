package com.ismartcoding.plain.ui.page.appfiles.components

import com.ismartcoding.plain.i18n.*

import androidx.navigation.NavHostController
import com.ismartcoding.plain.lib.extensions.isAudioFast
import com.ismartcoding.plain.lib.extensions.isImageFast
import com.ismartcoding.plain.lib.extensions.isPdfFile
import com.ismartcoding.plain.lib.extensions.isTextFile
import com.ismartcoding.plain.lib.extensions.isVideoFast
import com.ismartcoding.plain.helpers.coMain
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.db.DMessageFile
import com.ismartcoding.plain.extensions.resolveAppFileRealPath
import com.ismartcoding.plain.platform.fileToUriString
import com.ismartcoding.plain.platform.playAudioWithNotificationCheck
import com.ismartcoding.plain.platform.playlistAudioFromPath
import com.ismartcoding.plain.ui.components.mediaviewer.PreviewItem
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.TransformItemState
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModel
import com.ismartcoding.plain.ui.models.MediaPreviewData
import com.ismartcoding.plain.ui.models.VAppFile
import com.ismartcoding.plain.ui.nav.navigateOtherFile
import com.ismartcoding.plain.ui.nav.navigatePdf
import com.ismartcoding.plain.ui.nav.navigateTextFile

fun openAppFile(
    files: List<VAppFile>,
    file: VAppFile,
    navController: NavHostController,
    previewerState: MediaPreviewerState,
    itemState: TransformItemState,
    audioPlaylistVM: AudioPlaylistViewModel,
) {
    val path = file.appFile.realPath.resolveAppFileRealPath()
    val fileName = file.fileName

    when {
        fileName.isImageFast() || fileName.isVideoFast() -> {
            coMain {
                val previewItems = withIO {
                    files.filter { it.fileName.isImageFast() || it.fileName.isVideoFast() }.map {
                        val p = it.appFile.realPath.resolveAppFileRealPath()
                        PreviewItem(
                            it.appFile.id,
                            p,
                            it.appFile.size,
                            data = DMessageFile(uri = p, size = it.appFile.size, fileName = it.fileName),
                        )
                    }
                }
                withIO {
                    MediaPreviewData.setDataAsync(
                        itemState,
                        previewItems,
                        PreviewItem(file.appFile.id, path, file.appFile.size, data = DMessageFile(uri = path, size = file.appFile.size, fileName = fileName)),
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
                    val audio = withIO { playlistAudioFromPath(path) }
                    audioPlaylistVM.playlistItems.value = listOf(audio)
                    audioPlaylistVM.selectedPath.value = path
                    playAudioWithNotificationCheck(path)
                } catch (ex: Exception) {
                    DialogHelper.showMessage(Res.string.audio_play_error)
                }
            }
        }

        fileName.isTextFile() -> {
            if (file.appFile.size <= Constants.MAX_READABLE_TEXT_FILE_SIZE) {
                navController.navigateTextFile(path, fileName)
            } else {
                DialogHelper.showMessage(Res.string.text_file_size_limit)
            }
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