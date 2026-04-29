package com.ismartcoding.plain.ui.page.files

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import com.ismartcoding.plain.data.DFavoriteFolder
import com.ismartcoding.plain.features.file.DFile
import com.ismartcoding.plain.helpers.ShareHelper
import com.ismartcoding.plain.preferences.FavoriteFoldersPreference
import com.ismartcoding.plain.ui.base.ActionButtons
import com.ismartcoding.plain.ui.base.IconTextCopyButton
import com.ismartcoding.plain.ui.base.IconTextCutButton
import com.ismartcoding.plain.ui.base.IconTextDeleteButton
import com.ismartcoding.plain.ui.base.IconTextFavoriteButton
import com.ismartcoding.plain.ui.base.IconTextOpenWithButton
import com.ismartcoding.plain.ui.base.IconTextRenameButton
import com.ismartcoding.plain.ui.base.IconTextSelectButton
import com.ismartcoding.plain.ui.base.IconTextShareButton
import com.ismartcoding.plain.ui.base.IconTextZipButton
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.FilesViewModel
import com.ismartcoding.plain.ui.models.enterSelectMode
import com.ismartcoding.plain.ui.models.select
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
internal fun FileInfoActionButtons(
    file: DFile,
    filesVM: FilesViewModel,
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit,
    showRenameDialog: MutableState<Boolean>,
    context: android.content.Context,
    scope: CoroutineScope,
    onDismiss: () -> Unit,
    onShowPasteBar: (Boolean) -> Unit,
) {
    ActionButtons {
        if (!filesVM.showSearchBar.value) {
            IconTextSelectButton {
                filesVM.enterSelectMode()
                filesVM.select(file.path)
                onDismiss()
            }
        }

        IconTextCutButton {
            performCutFiles(filesVM, listOf(file), onShowPasteBar) { onDismiss() }
        }

        IconTextCopyButton {
            performCopyFiles(filesVM, listOf(file), onShowPasteBar) { onDismiss() }
        }

        if (file.isDir) {
            IconTextFavoriteButton(isFavorite = isFavorite) {
                onFavoriteToggle()
            }
        }

        IconTextShareButton {
            ShareHelper.sharePaths(context, setOf(file.path))
            onDismiss()
        }
        if (!file.isDir) {
            IconTextOpenWithButton {
                ShareHelper.openPathWith(context, file.path)
            }
        }
        if (!file.isDir) {
            IconTextZipButton {
                performZipFiles(scope, context, filesVM, listOf(file)) { onDismiss() }
            }
        }
        IconTextRenameButton {
            showRenameDialog.value = true
        }
        IconTextDeleteButton {
            DialogHelper.confirmToDelete {
                filesVM.deleteFiles(setOf(file.path))
                onDismiss()
            }
        }
    }
}

