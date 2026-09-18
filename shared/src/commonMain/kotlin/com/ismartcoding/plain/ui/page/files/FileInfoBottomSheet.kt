package com.ismartcoding.plain.ui.page.files
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import com.ismartcoding.plain.ui.theme.PlainTheme

import com.ismartcoding.plain.i18n.*

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ismartcoding.plain.lib.extensions.formatBytes
import com.ismartcoding.plain.lib.extensions.getFilenameFromPath
import com.ismartcoding.plain.lib.extensions.getFilenameExtension
import com.ismartcoding.plain.lib.extensions.getMimeType
import com.ismartcoding.plain.platform.formatDateTime
import com.ismartcoding.plain.platform.getFileIconPath
import com.ismartcoding.plain.platform.renameAndScanFile
import com.ismartcoding.plain.preferences.FavoriteFoldersPreference
import com.ismartcoding.plain.data.DFavoriteFolder
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.CopyIconButton
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import com.ismartcoding.plain.ui.base.PSheetHeader
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.components.FileRenameDialog
import com.ismartcoding.plain.ui.models.FilesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileInfoBottomSheet(filesVM: FilesViewModel) {
    val scope = rememberCoroutineScope()
    val file = filesVM.selectedFile.value ?: return
    // Scoped to this sheet instance: a fresh long-press always starts with the dialog
    // closed, so a stale flag can never pop rename together with the sheet.
    val showRenameDialog = remember { mutableStateOf(false) }
    var isFavorite by remember { mutableStateOf(false) }
    val onDismiss = { filesVM.selectedFile.value = null }

    LaunchedEffect(file.path) {
        if (file.isDir) {
            isFavorite = FavoriteFoldersPreference.isFavoriteAsync(file.path)
        }
    }

    if (showRenameDialog.value) {
        FileRenameDialog(path = file.path, onDismiss = {
            showRenameDialog.value = false
        }, onRename = { p, name -> renameAndScanFile(p, name) }, onRenamed = {
            file.name = it.getFilenameFromPath()
            file.path = it
            filesVM.selectedFile.value = null
            filesVM.loadAsync()
        })
    }

    PModalBottomSheet(onDismissRequest = { onDismiss() }) {
        LazyColumn {
            item { VerticalSpace(16.dp) }
            item {
                PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
                    PSheetHeader(
                        thumbnail = {
                            AsyncImage(
                                model = if (file.isDir) getFileIconPath("folder") else getFileIconPath(file.path.getFilenameExtension()),
                                contentDescription = file.name,
                                modifier = Modifier.size(44.dp),
                            )
                        },
                        title = file.name,
                        subtitle = if (file.isDir) {
                            stringResource(Res.string.folder) + " · " + pluralStringResource(Res.plurals.items, file.children, file.children)
                        } else {
                            file.path.getMimeType() + " · " + file.size.formatBytes()
                        },
                    )
                    FileInfoPrimaryActions(
                        file = file, filesVM = filesVM, onDismiss = onDismiss,
                        onShowPasteBar = { filesVM.showPasteBar.value = it },
                    )
                }
                VerticalSpace(12.dp)
                FileInfoSecondaryActions(
                    file = file, filesVM = filesVM, isFavorite = isFavorite,
                    onFavoriteToggle = {
                        scope.launch(Dispatchers.Default) {
                            if (isFavorite) {
                                FavoriteFoldersPreference.removeAsync(file.path)
                                isFavorite = false
                            } else {
                                FavoriteFoldersPreference.addAsync(DFavoriteFolder(rootPath = filesVM.rootPath, fullPath = file.path))
                                isFavorite = true
                            }
                            filesVM.favoriteFoldersVersion.value++
                        }
                    },
                    showRenameDialog = showRenameDialog,
                    scope = scope,
                    onShowPasteBar = { filesVM.showPasteBar.value = it },
                    onDismiss = onDismiss,
                )
                VerticalSpace(12.dp)
                PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
                    PListItem(title = file.path, action = {
                        CopyIconButton(text = file.path, clipLabel = stringResource(Res.string.file_path))
                    })
                    PListItem(title = stringResource(Res.string.updated_at), value = file.updatedAt.formatDateTime())
                    file.createdAt?.let {
                        PListItem(title = stringResource(Res.string.created_at), value = it.formatDateTime())
                    }
                }
            }
            item { BottomSpace() }
        }
    }
}
