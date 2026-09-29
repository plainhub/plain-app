package com.ismartcoding.plain.ui.page.appfiles.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.ismartcoding.plain.extensions.resolveAppFileRealPath
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.lib.coMain
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.platform.LocaleHelper
import com.ismartcoding.plain.platform.copyFileToDir
import com.ismartcoding.plain.platform.openFileExternal
import com.ismartcoding.plain.platform.saveFileToDownloads
import com.ismartcoding.plain.platform.shareFiles
import com.ismartcoding.plain.ui.base.PSheetPrimaryAction
import com.ismartcoding.plain.ui.base.PSheetPrimaryActionsCard
import com.ismartcoding.plain.ui.components.SaveToSheet
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.VAppFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.forward as ui_drawable_forward
import com.ismartcoding.plain.ui.resources.save as ui_drawable_save
import com.ismartcoding.plain.ui.resources.share_2 as ui_drawable_share_2
import com.ismartcoding.plain.ui.resources.square_arrow_out_up_right as ui_drawable_square_arrow_out_up_right
import com.ismartcoding.plain.i18n.save
import com.ismartcoding.plain.i18n.forward

@Composable
fun AppFileInfoActionButtons(
    file: VAppFile,
    onShowForwardDialog: () -> Unit,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var showSaveSheet by remember { mutableStateOf(false) }
    val realPath = file.appFile.realPath.resolveAppFileRealPath()

    PSheetPrimaryActionsCard {
        PSheetPrimaryAction(UiRes.drawable.ui_drawable_forward, stringResource(Res.string.forward)) {
            onShowForwardDialog()
        }
        PSheetPrimaryAction(UiRes.drawable.ui_drawable_share_2, stringResource(Res.string.share)) {
            scope.launch(Dispatchers.Default) {
                shareFiles(listOf(realPath))
            }
            onDismiss()
        }
        PSheetPrimaryAction(UiRes.drawable.ui_drawable_save, stringResource(Res.string.save_as)) {
            showSaveSheet = true
        }
        PSheetPrimaryAction(UiRes.drawable.ui_drawable_square_arrow_out_up_right, stringResource(Res.string.open_with)) {
            openFileExternal(realPath)
        }
    }

    if (showSaveSheet) {
        SaveToSheet(
            title = file.fileName,
            onDismiss = { showSaveSheet = false },
            onDownloads = {
                showSaveSheet = false
                coMain {
                    val result = withIO { saveFileToDownloads(realPath, file.fileName) }
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
                    val result = withIO { copyFileToDir(realPath, dir, file.fileName) }
                    if (result.isNotEmpty()) {
                        DialogHelper.showConfirmDialog("", LocaleHelper.getStringFAsync(Res.string.file_save_to, result))
                    } else {
                        DialogHelper.showErrorMessage(result)
                    }
                }
            },
        )
    }
}
