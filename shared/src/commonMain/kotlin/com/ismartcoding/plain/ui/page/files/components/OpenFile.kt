package com.ismartcoding.plain.ui.page.files.components

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
import com.ismartcoding.plain.features.file.DFile
import com.ismartcoding.plain.features.file.ZipBrowserHelper
import com.ismartcoding.plain.platform.extractZipEntryToCache
import com.ismartcoding.plain.platform.fileToUriString
import com.ismartcoding.plain.platform.openFileExternal
import com.ismartcoding.plain.platform.playAudioWithNotificationCheck
import com.ismartcoding.plain.platform.playlistAudioFromPath
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.TransformItemState
import com.ismartcoding.plain.ui.extensions.toPreviewItem
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModel
import com.ismartcoding.plain.ui.models.MediaPreviewData
import com.ismartcoding.plain.ui.nav.navigatePdf
import com.ismartcoding.plain.ui.nav.navigateTextFile

fun openFile(
    files: List<DFile>,
    file: DFile,
    navController: NavHostController,
    previewerState: MediaPreviewerState,
    itemState: TransformItemState,
    audioPlaylistVM: AudioPlaylistViewModel? = null,
) {
    // For files inside a zip archive, extract to the cache dir first, then open normally.
    if (ZipBrowserHelper.isZipPath(file.path)) {
        coMain {
            DialogHelper.showLoading()
            val tempPath = withIO { extractZipEntryToCache(file.path) }
            DialogHelper.hideLoading()
            if (tempPath == null) {
                DialogHelper.showMessage(Res.string.error)
                return@coMain
            }
            val extracted = file.copy(path = tempPath)
            when {
                tempPath.isImageFast() || tempPath.isVideoFast() -> {
                    // itemState is registered via TransformImageView (using the cached path)
                    // so we can use openTransform for the zoom-from-thumbnail animation.
                    withIO {
                        MediaPreviewData.setDataAsync(
                            itemState,
                            listOf(extracted).map { it.toPreviewItem() },
                            extracted.toPreviewItem(),
                        )
                    }
                    previewerState.openTransform(
                        index = 0,
                        itemState = itemState,
                    )
                }
                else -> {
                    // audio, text, PDF — real temp path works normally
                    openFile(listOf(extracted), extracted, navController, previewerState, itemState, audioPlaylistVM)
                }
            }
        }
        return
    }

    val path = file.path

    when {
        path.isImageFast() || path.isVideoFast() -> {
            coMain {
                withIO {
                    MediaPreviewData.setDataAsync(
                            itemState,
                            files.filter { it.path.isImageFast() || it.path.isVideoFast() }.map { it.toPreviewItem() },
                            file.toPreviewItem(),
                        )
                }
                previewerState.openTransform(
                    index = MediaPreviewData.items.indexOfFirst { it.id == file.path },
                    itemState = itemState,
                )
            }
        }

        path.isAudioFast() -> {
            try {
                if (audioPlaylistVM != null) {
                    val audio = playlistAudioFromPath(path)
                    audioPlaylistVM.playlistItems.value = listOf(audio)
                    audioPlaylistVM.selectedPath.value = path
                }
                playAudioWithNotificationCheck(path)
            } catch (ex: Exception) {
                DialogHelper.showMessage(Res.string.audio_play_error)
            }
        }

        path.isTextFile() -> {
            if (file.size <= Constants.MAX_READABLE_TEXT_FILE_SIZE) {
                navController.navigateTextFile(path)
            } else {
                DialogHelper.showMessage(Res.string.text_file_size_limit)
            }
        }

        path.isPdfFile() -> {
            try {
                navController.navigatePdf(fileToUriString(path))
            } catch (ex: Exception) {
                DialogHelper.showMessage(Res.string.pdf_open_error)
            }
        }

        else -> {
            openFileExternal(path)
        }
    }
}
