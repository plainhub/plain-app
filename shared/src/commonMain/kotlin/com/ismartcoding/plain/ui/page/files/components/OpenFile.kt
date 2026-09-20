package com.ismartcoding.plain.ui.page.files.components

import com.ismartcoding.plain.i18n.*

import androidx.navigation.NavHostController
import com.ismartcoding.plain.lib.extensions.isAudioFast
import com.ismartcoding.plain.lib.extensions.isImageFast
import com.ismartcoding.plain.lib.extensions.isPdfFile
import com.ismartcoding.plain.lib.extensions.isTextFile
import com.ismartcoding.plain.lib.extensions.isVideoFast
import com.ismartcoding.plain.lib.coMain
import com.ismartcoding.plain.lib.withIO
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

    openLocalFileByType(
        path = path,
        navController = navController,
        audioPlaylistVM = audioPlaylistVM,
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

/**
 * Type dispatch shared by every "open a local file" flow (FilesPage, zip
 * browser, shared-folder browser): media → [onPreviewMedia] (the caller owns
 * how its previewer opens), audio → the audio player, text/PDF → their
 * pages, anything else → [onUnsupported]. [mediaHint] forces the media
 * branch when the path lacks a recognizable media extension (e.g. a shared
 * entry typed only by its MIME).
 */
fun openLocalFileByType(
    path: String,
    navController: NavHostController,
    audioPlaylistVM: AudioPlaylistViewModel? = null,
    mediaHint: Boolean = false,
    onPreviewMedia: () -> Unit,
    onUnsupported: () -> Unit = {},
) {
    when {
        path.isImageFast() || path.isVideoFast() || mediaHint -> onPreviewMedia()

        path.isAudioFast() -> {
            try {
                if (audioPlaylistVM != null) {
                    val audio = playlistAudioFromPath(path)
                    coMain { audioPlaylistVM.playSingleAsync(audio) }
                }
                playAudioWithNotificationCheck(path)
            } catch (ex: Exception) {
                DialogHelper.showMessage(Res.string.audio_play_error)
            }
        }

        path.isTextFile() -> {
            navController.navigateTextFile(path)
        }

        path.isPdfFile() -> {
            try {
                navController.navigatePdf(fileToUriString(path))
            } catch (ex: Exception) {
                DialogHelper.showMessage(Res.string.pdf_open_error)
            }
        }

        else -> {
            onUnsupported()
        }
    }
}
