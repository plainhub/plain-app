package com.ismartcoding.plain.ui.page.files.components

import android.content.Context
import androidx.core.net.toUri
import androidx.navigation.NavHostController
import com.ismartcoding.lib.extensions.isAudioFast
import com.ismartcoding.lib.extensions.isImageFast
import com.ismartcoding.lib.extensions.isPdfFile
import com.ismartcoding.lib.extensions.isTextFile
import com.ismartcoding.lib.extensions.isVideoFast
import com.ismartcoding.lib.helpers.CoroutinesHelper.coMain
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.R
import com.ismartcoding.plain.audio.DPlaylistAudio
import com.ismartcoding.plain.audio.AudioPlayer
import com.ismartcoding.plain.features.Permissions
import com.ismartcoding.plain.features.file.DFile
import com.ismartcoding.plain.features.file.ZipBrowserHelper
import com.ismartcoding.plain.helpers.ShareHelper
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.TransformItemState
import com.ismartcoding.plain.ui.extensions.toPreviewItem
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModel
import com.ismartcoding.plain.ui.models.MediaPreviewData
import com.ismartcoding.plain.ui.nav.navigatePdf
import com.ismartcoding.plain.ui.nav.navigateTextFile
import java.io.File

fun openFile(
    context: Context,
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
            val tempFile = withIO { ZipBrowserHelper.extractEntryToCache(context, file.path) }
            DialogHelper.hideLoading()
            if (tempFile == null) {
                DialogHelper.showMessage(R.string.error)
                return@coMain
            }
            val tempPath = tempFile.absolutePath
            val extracted = file.copy(path = tempPath)
            when {
                tempPath.isImageFast() || tempPath.isVideoFast() -> {
                    // itemState is registered via TransformImageView (using the cached path)
                    // so we can use openTransform for the zoom-from-thumbnail animation.
                    withIO {
                        MediaPreviewData.setDataAsync(
                            context, itemState,
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
                    openFile(context, listOf(extracted), extracted, navController, previewerState, itemState, audioPlaylistVM)
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
                        context, itemState,
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
                Permissions.checkNotification(context, R.string.audio_notification_prompt) {
                    val audio = DPlaylistAudio.fromPath(context, path)
                    if (audioPlaylistVM != null) {
                        coMain {
                            withIO { audioPlaylistVM.playlistItems.value = listOf(audio) }
                            audioPlaylistVM.selectedPath.value = path
                            AudioPlayer.play(context, audio)
                        }
                    } else {
                        AudioPlayer.play(context, audio)
                    }
                }
            } catch (ex: Exception) {
                DialogHelper.showMessage(R.string.audio_play_error)
            }
        }

        path.isTextFile() -> {
            if (file.size <= Constants.MAX_READABLE_TEXT_FILE_SIZE) {
                navController.navigateTextFile(path)
            } else {
                DialogHelper.showMessage(R.string.text_file_size_limit)
            }
        }

        path.isPdfFile() -> {
            try {
                navController.navigatePdf(File(path).toUri())
            } catch (ex: Exception) {
                DialogHelper.showMessage(R.string.pdf_open_error)
            }
        }

        else -> {
            ShareHelper.openPathWith(context, path)
        }
    }
}
