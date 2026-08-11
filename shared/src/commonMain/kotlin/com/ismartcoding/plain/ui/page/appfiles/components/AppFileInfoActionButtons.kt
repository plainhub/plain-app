package com.ismartcoding.plain.ui.page.appfiles.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.ismartcoding.plain.extensions.resolveAppFileRealPath
import com.ismartcoding.plain.platform.openFileExternal
import com.ismartcoding.plain.platform.shareFiles
import com.ismartcoding.plain.ui.base.ActionButtons
import com.ismartcoding.plain.ui.base.IconTextForwardButton
import com.ismartcoding.plain.ui.base.IconTextOpenWithButton
import com.ismartcoding.plain.ui.base.IconTextShareButton
import com.ismartcoding.plain.ui.models.VAppFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun AppFileInfoActionButtons(
    file: VAppFile,
    onShowForwardDialog: () -> Unit,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    ActionButtons {
        IconTextForwardButton {
            onShowForwardDialog()
        }
        IconTextShareButton {
            scope.launch(Dispatchers.Default) {
                shareFiles(listOf(file.appFile.realPath.resolveAppFileRealPath()))
            }
            onDismiss()
        }
        IconTextOpenWithButton {
            openFileExternal(file.appFile.realPath.resolveAppFileRealPath())
        }
    }
}