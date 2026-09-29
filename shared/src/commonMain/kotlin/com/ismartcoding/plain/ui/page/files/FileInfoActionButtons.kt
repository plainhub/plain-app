package com.ismartcoding.plain.ui.page.files

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
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
import com.ismartcoding.plain.ui.base.PSheetPrimaryDeleteAction
import com.ismartcoding.plain.ui.base.PSheetPrimaryActionsRow
import com.ismartcoding.plain.ui.helpers.confirmActionAsync
import com.ismartcoding.plain.ui.models.FilesViewModel
import com.ismartcoding.plain.ui.models.enterSelectMode
import com.ismartcoding.plain.ui.models.select
import com.ismartcoding.plain.ui.theme.PlainTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.check as ui_drawable_check
import com.ismartcoding.plain.ui.resources.copy as ui_drawable_copy
import com.ismartcoding.plain.ui.resources.link as ui_drawable_link
import com.ismartcoding.plain.ui.resources.list_checks as ui_drawable_list_checks
import com.ismartcoding.plain.ui.resources.package2 as ui_drawable_package2
import com.ismartcoding.plain.ui.resources.pen as ui_drawable_pen
import com.ismartcoding.plain.ui.resources.plus as ui_drawable_plus
import com.ismartcoding.plain.ui.resources.scissors as ui_drawable_scissors
import com.ismartcoding.plain.ui.resources.share_2 as ui_drawable_share_2
import com.ismartcoding.plain.ui.resources.square_arrow_out_up_right as ui_drawable_square_arrow_out_up_right
import com.ismartcoding.plain.i18n.link
import com.ismartcoding.plain.i18n.copy

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
            PSheetPrimaryAction(UiRes.drawable.ui_drawable_list_checks, stringResource(Res.string.select)) {
                filesVM.enterSelectMode()
                filesVM.select(file.path)
                onDismiss()
            }
        }
        PSheetPrimaryAction(UiRes.drawable.ui_drawable_share_2, stringResource(Res.string.share)) {
            shareFiles(listOf(file.path))
            onDismiss()
        }
        PSheetPrimaryAction(UiRes.drawable.ui_drawable_copy, stringResource(Res.string.copy)) {
            performCopyFiles(filesVM, listOf(file), onShowPasteBar) { onDismiss() }
        }
        PSheetPrimaryDeleteAction(stringResource(Res.string.delete)) {
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
    onShowPasteBar: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
        Column {
            PSheetActionRow(UiRes.drawable.ui_drawable_pen, stringResource(Res.string.rename)) {
                showRenameDialog.value = true
            }
            PSheetActionRow(UiRes.drawable.ui_drawable_scissors, stringResource(Res.string.cut)) {
                performCutFiles(filesVM, listOf(file), onShowPasteBar) { onDismiss() }
            }
            PSheetActionRow(UiRes.drawable.ui_drawable_link, stringResource(Res.string.share_link)) {
                filesVM.sharePaths.clear()
                filesVM.sharePaths.add(file.path)
                onDismiss()
                filesVM.showCreateShareDialog.value = true
            }
            if (file.isDir) {
                PSheetActionRow(
                    if (isFavorite) UiRes.drawable.ui_drawable_check else UiRes.drawable.ui_drawable_plus,
                    stringResource(Res.string.favorites),
                ) {
                    onFavoriteToggle()
                }
            }
            if (!file.isDir) {
                PSheetActionRow(UiRes.drawable.ui_drawable_square_arrow_out_up_right, stringResource(Res.string.open_with)) {
                    openFileExternal(file.path)
                }
                PSheetActionRow(UiRes.drawable.ui_drawable_package2, stringResource(Res.string.compress)) {
                    performZipFiles(scope, filesVM, listOf(file)) { onDismiss() }
                }
            }
        }
    }
}
