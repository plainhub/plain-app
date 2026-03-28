package com.ismartcoding.plain.ui.page.appfiles.components

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
import com.ismartcoding.plain.db.DMessageFile
import com.ismartcoding.plain.helpers.ShareHelper
import com.ismartcoding.plain.ui.components.mediaviewer.PreviewItem
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.TransformItemState
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.MediaPreviewData
import com.ismartcoding.plain.ui.models.VAppFile
import com.ismartcoding.plain.ui.nav.navigateOtherFile
import com.ismartcoding.plain.ui.nav.navigatePdf
import com.ismartcoding.plain.ui.nav.navigateTextFile
import java.io.File

fun openAppFile(
    context: Context,
    files: List<VAppFile>,
    file: VAppFile,
    navController: NavHostController,
    previewerState: MediaPreviewerState,
    itemState: TransformItemState,
) {
    val path = file.appFile.realPath
    val fileName = file.fileName

    when {
        fileName.isImageFast() || fileName.isVideoFast() -> {
            coMain {
                val previewItems = withIO {
                    files.filter { it.fileName.isImageFast() || it.fileName.isVideoFast() }.map {
                        PreviewItem(
                            it.appFile.id,
                            it.appFile.realPath,
                            it.appFile.size,
                            data = DMessageFile(uri = it.appFile.realPath, size = it.appFile.size, fileName = it.fileName),
                        )
                    }
                }
                withIO {
                    MediaPreviewData.setDataAsync(
                        context,
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

        fileName.isAudioFast() -> ShareHelper.openPathWith(context, path)

        fileName.isTextFile() -> {
            if (file.appFile.size <= Constants.MAX_READABLE_TEXT_FILE_SIZE) {
                navController.navigateTextFile(path, fileName)
            } else {
                DialogHelper.showMessage(R.string.text_file_size_limit)
            }
        }

        fileName.isPdfFile() -> {
            try {
                navController.navigatePdf(File(path).toUri())
            } catch (ex: Exception) {
                DialogHelper.showMessage(R.string.pdf_open_error)
            }
        }

        else -> navController.navigateOtherFile(path, fileName)
    }
}