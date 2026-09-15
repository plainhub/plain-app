package com.ismartcoding.plain.ui.page.appfiles.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.ismartcoding.plain.extensions.resolveAppFileRealPath
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.platform.openFileExternal
import com.ismartcoding.plain.platform.shareFiles
import com.ismartcoding.plain.ui.base.PSheetPrimaryAction
import com.ismartcoding.plain.ui.base.PSheetPrimaryActionsCard
import com.ismartcoding.plain.ui.models.VAppFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun AppFileInfoActionButtons(
    file: VAppFile,
    onShowForwardDialog: () -> Unit,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    PSheetPrimaryActionsCard {
        PSheetPrimaryAction(Res.drawable.forward, stringResource(Res.string.forward)) {
            onShowForwardDialog()
        }
        PSheetPrimaryAction(Res.drawable.share_2, stringResource(Res.string.share)) {
            scope.launch(Dispatchers.Default) {
                shareFiles(listOf(file.appFile.realPath.resolveAppFileRealPath()))
            }
            onDismiss()
        }
        PSheetPrimaryAction(Res.drawable.square_arrow_out_up_right, stringResource(Res.string.open_with)) {
            openFileExternal(file.appFile.realPath.resolveAppFileRealPath())
        }
    }
}
