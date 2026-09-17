package com.ismartcoding.plain.ui.page.files

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import com.ismartcoding.plain.features.file.DFile
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.platform.openFileExternal
import com.ismartcoding.plain.platform.shareFiles
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PSheetActionRow
import com.ismartcoding.plain.ui.base.PSheetPrimaryAction
import com.ismartcoding.plain.ui.base.PSheetPrimaryActionsRow
import com.ismartcoding.plain.ui.helpers.confirmActionAsync
import com.ismartcoding.plain.ui.models.FilesViewModel
import com.ismartcoding.plain.ui.models.enterSelectMode
import com.ismartcoding.plain.ui.models.select
import com.ismartcoding.plain.ui.theme.PlainTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun FileInfoPrimaryActions(
    file: DFile,
    filesVM: FilesViewModel,
    onDismiss: () -> Unit,
    onShowPasteBar: (Boolean) -> Unit,
) {
    val scope = rememberCoroutineScope()
    PSheetPrimaryActionsRow {
        if (!filesVM.showSearchBar.value) {
            PSheetPrimaryAction(Res.drawable.list_checks, stringResource(Res.string.select)) {
                filesVM.enterSelectMode()
                filesVM.select(file.path)
                onDismiss()
            }
        }
        PSheetPrimaryAction(Res.drawable.share_2, stringResource(Res.string.share)) {
            shareFiles(listOf(file.path))
            onDismiss()
        }
        PSheetPrimaryAction(Res.drawable.copy, stringResource(Res.string.copy)) {
            performCopyFiles(filesVM, listOf(file), onShowPasteBar) { onDismiss() }
        }
        PSheetPrimaryAction(Res.drawable.scissors, stringResource(Res.string.cut)) {
            performCutFiles(filesVM, listOf(file), onShowPasteBar) { onDismiss() }
        }
        PSheetPrimaryAction(
            Res.drawable.delete_forever,
            stringResource(Res.string.delete),
            container = MaterialTheme.colorScheme.errorContainer,
            tint = MaterialTheme.colorScheme.error,
        ) {
            scope.launch {
                confirmActionAsync(
                    Res.string.delete,
                    Res.string.confirm_to_delete,
                    callback = {
                        filesVM.deleteFiles(setOf(file.path))
                        onDismiss()
                    },
                    danger = true
                )
            }
        }
    }
}

@Composable
internal fun FileInfoSecondaryActions(
    file: DFile,
    filesVM: FilesViewModel,
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit,
    showRenameDialog: MutableState<Boolean>,
    scope: CoroutineScope,
    onDismiss: () -> Unit,
) {
    PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
        Column {
            PSheetActionRow(Res.drawable.pen, stringResource(Res.string.rename)) {
                showRenameDialog.value = true
            }
            PSheetActionRow(Res.drawable.link, stringResource(Res.string.share_link)) {
                filesVM.sharePaths.clear()
                filesVM.sharePaths.add(file.path)
                onDismiss()
                filesVM.showCreateShareDialog.value = true
            }
            if (file.isDir) {
                PSheetActionRow(
                    if (isFavorite) Res.drawable.check else Res.drawable.plus,
                    stringResource(Res.string.favorites),
                ) {
                    onFavoriteToggle()
                }
            }
            if (!file.isDir) {
                PSheetActionRow(Res.drawable.square_arrow_out_up_right, stringResource(Res.string.open_with)) {
                    openFileExternal(file.path)
                }
                PSheetActionRow(Res.drawable.package2, stringResource(Res.string.compress)) {
                    performZipFiles(scope, filesVM, listOf(file)) { onDismiss() }
                }
            }
        }
    }
}
