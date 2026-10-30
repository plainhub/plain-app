package com.ismartcoding.plain.ui.files.components

import com.ismartcoding.plain.features.media.MediaClient

import com.ismartcoding.plain.features.files.FilesClient

import com.ismartcoding.plain.features.files.extension

import com.ismartcoding.plain.models.File as FileEntry

import com.ismartcoding.plain.i18n.*

import androidx.navigation.NavHostController
import com.ismartcoding.plain.lib.extensions.isAudioFast
import com.ismartcoding.plain.lib.extensions.isImageFast
import com.ismartcoding.plain.lib.extensions.isPdfFile
import com.ismartcoding.plain.lib.extensions.isTextFile
import com.ismartcoding.plain.lib.extensions.isVideoFast
import com.ismartcoding.plain.lib.coMain
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.features.files.ZipBrowserHelper
import com.ismartcoding.plain.platform.files.fileToUriString
import com.ismartcoding.plain.platform.system.openFileExternal
import com.ismartcoding.plain.platform.media.playAudioWithNotificationCheck
import com.ismartcoding.plain.ui.media.viewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.media.viewer.previewer.TransformItemState
import com.ismartcoding.plain.ui.media.extensions.toPreviewItem
import com.ismartcoding.plain.ui.components.DialogHelper
import com.ismartcoding.plain.ui.audio.AudioQueueViewModel
import com.ismartcoding.plain.ui.media.MediaPreviewData
import com.ismartcoding.plain.ui.navigation.navigateOtherFile
import com.ismartcoding.plain.ui.navigation.navigatePdf
import com.ismartcoding.plain.ui.navigation.navigateTextFile
import kotlinx.coroutines.CancellationException

fun openFile(
    files: List<FileEntry>,
    file: FileEntry,
    navController: NavHostController,
    previewerState: MediaPreviewerState,
    itemState: TransformItemState,
    audioQueueVM: AudioQueueViewModel? = null,
    source: com.ismartcoding.plain.rust.graphql.ContentSource = com.ismartcoding.plain.rust.graphql.ContentSource.Local,
) {
    if (source is com.ismartcoding.plain.rust.graphql.ContentSource.Remote) {
        coMain {
            DialogHelper.showLoading()
            try {
                val path = if (ZipBrowserHelper.isZipPath(file.path)) source.files.cacheZipEntry(file.path) ?: error("archive_entry_unavailable") else file.path
                when {
                    file.name.isImageFast() || file.name.isVideoFast() -> {
                        val previewFiles = if (ZipBrowserHelper.isZipPath(file.path)) listOf(file.copy(path = path)) else files.filter { it.path.isImageFast() || it.path.isVideoFast() }
                        val selected = file.copy(path = path).toPreviewItem(source)
                        MediaPreviewData.setDataAsync(itemState, previewFiles.map { it.toPreviewItem(source) }, selected)
                        previewerState.openTransform(index = MediaPreviewData.items.indexOfFirst { it.id == path }, itemState = itemState)
                    }
                    else -> openFileByType(
                        path, navController, source = source, name = file.name,
                        onPreviewMedia = {},
                        onUnsupported = { navController.navigateOtherFile(path, file.name) },
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                DialogHelper.showMessage(error)
            } finally {
                DialogHelper.hideLoading()
            }
        }
        return
    }
    // For files inside a zip archive, extract to the cache dir first, then open normally.
    if (ZipBrowserHelper.isZipPath(file.path)) {
        coMain {
            DialogHelper.showLoading()
            val tempPath = withIO { FilesClient.cacheZipEntry(file.path) }
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
                    openFile(listOf(extracted), extracted, navController, previewerState, itemState, audioQueueVM)
                }
            }
        }
        return
    }

    val path = file.path

    openFileByType(
        path = path,
        navController = navController,
        audioQueueVM = audioQueueVM,
        onPreviewMedia = {
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
        },
        onUnsupported = { openFileExternal(path) },
    )
}

fun openFileByType(
    path: String,
    navController: NavHostController,
    audioQueueVM: AudioQueueViewModel? = null,
    mediaHint: Boolean = false,
    source: com.ismartcoding.plain.rust.graphql.ContentSource = com.ismartcoding.plain.rust.graphql.ContentSource.Local,
    name: String = path.substringAfterLast('/'),
    onPreviewMedia: () -> Unit,
    onUnsupported: () -> Unit = {},
) {
    when {
        name.isImageFast() || name.isVideoFast() || mediaHint -> onPreviewMedia()

        name.isAudioFast() -> {
            coMain {
                try {
                    if (source is com.ismartcoding.plain.rust.graphql.ContentSource.Remote) {
                        val audio = source.media.audioInfo(path)
                        com.ismartcoding.plain.platform.media.audioJustPlayWithNotificationCheck(audio, source)
                        return@coMain
                    }
                    if (audioQueueVM != null) {
                        val audio = MediaClient.audioInfo(path)
                        audioQueueVM.playSingleAsync(audio)
                    }
                    playAudioWithNotificationCheck(path)
                } catch (ex: CancellationException) {
                    throw ex
                } catch (ex: Exception) {
                    DialogHelper.showMessage(Res.string.audio_play_error)
                }
            }
        }
        name.isTextFile() -> {
            navController.navigateTextFile(path, name)
        }

        name.isPdfFile() -> coMain {
            try {
                navController.navigatePdf(fileToUriString(source.fileForOpen(path, name)))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (ex: Exception) {
                DialogHelper.showMessage(Res.string.pdf_open_error)
            }
        }

        else -> {
            onUnsupported()
        }
    }
}
