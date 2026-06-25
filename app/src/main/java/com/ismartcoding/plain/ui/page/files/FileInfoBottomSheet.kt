package com.ismartcoding.plain.ui.page.files
import com.ismartcoding.plain.preferences.*

import com.ismartcoding.plain.i18n.*

import android.content.ClipData
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.lib.extensions.formatBytes
import com.ismartcoding.plain.lib.extensions.getFilenameFromPath
import com.ismartcoding.plain.lib.extensions.getMimeType
import com.ismartcoding.plain.clipboardManager
import com.ismartcoding.plain.extensions.formatDateTime
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.preferences.FavoriteFoldersPreference
import com.ismartcoding.plain.data.DFavoriteFolder
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.CopyIconButton
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.components.FileRenameDialog
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.FilesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileInfoBottomSheet(filesVM: FilesViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val file = filesVM.selectedFile.value ?: return
    var isFavorite by remember { mutableStateOf(false) }
    val onDismiss = { filesVM.selectedFile.value = null }

    LaunchedEffect(file.path) {
        if (file.isDir) {
            isFavorite = FavoriteFoldersPreference.isFavoriteAsync(file.path)
        }
    }

    if (filesVM.showRenameDialog.value) {
        FileRenameDialog(path = file.path, onDismiss = {
            filesVM.showRenameDialog.value = false
        }, onDoneAsync = {
            file.name = it.getFilenameFromPath()
            file.path = it
            filesVM.selectedFile.value = null
            filesVM.loadAsync(context)
        })
    }

    PModalBottomSheet(onDismissRequest = { onDismiss() }) {
        LazyColumn {
            item { VerticalSpace(32.dp) }
            item {
                FileInfoActionButtons(
                    file = file, filesVM = filesVM, isFavorite = isFavorite,
                    onFavoriteToggle = {
                        scope.launch(Dispatchers.IO) {
                            if (isFavorite) {
                                FavoriteFoldersPreference.removeAsync(file.path)
                                isFavorite = false
                            } else {
                                FavoriteFoldersPreference.addAsync(DFavoriteFolder(rootPath = filesVM.rootPath, fullPath = file.path))
                                isFavorite = true
                            }
                        }
                    },
                    showRenameDialog = filesVM.showRenameDialog,
                    context = context, scope = scope, onDismiss = onDismiss,
                    onShowPasteBar = { filesVM.showPasteBar.value = it },
                )
                VerticalSpace(dp = 24.dp)
                PCard {
                    PListItem(title = file.path, action = {
                        CopyIconButton(text = file.path, clipLabel = stringResource(Res.string.file_path))
                    })
                }
                VerticalSpace(dp = 16.dp)
                PCard {
                    if (!file.isDir) {
                        PListItem(title = stringResource(Res.string.file_size), value = file.size.formatBytes())
                    }
                    PListItem(title = stringResource(Res.string.type), value = if (file.isDir) stringResource(Res.string.folder) else file.path.getMimeType())
                    file.createdAt?.let {
                        PListItem(title = stringResource(Res.string.created_at), value = it.formatDateTime())
                    }
                    PListItem(title = stringResource(Res.string.updated_at), value = file.updatedAt.formatDateTime())
                    if (file.isDir && file.children > 0) {
                        PListItem(title = stringResource(Res.string.items), value = file.children.toString())
                    }
                }
            }
            item { BottomSpace() }
        }
    }
}