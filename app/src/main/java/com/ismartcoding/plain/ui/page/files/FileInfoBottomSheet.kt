package com.ismartcoding.plain.ui.page.files

import android.content.ClipData
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.lib.extensions.formatBytes
import com.ismartcoding.lib.extensions.getFilenameFromPath
import com.ismartcoding.lib.extensions.getMimeType
import com.ismartcoding.plain.R
import com.ismartcoding.plain.clipboardManager
import com.ismartcoding.plain.data.DFavoriteFolder
import com.ismartcoding.plain.extensions.formatDateTime
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.helpers.ShareHelper
import com.ismartcoding.plain.preference.FavoriteFoldersPreference
import com.ismartcoding.plain.ui.base.ActionButtons
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.IconTextDeleteButton
import com.ismartcoding.plain.ui.base.IconTextFavoriteButton
import com.ismartcoding.plain.ui.base.IconTextRenameButton
import com.ismartcoding.plain.ui.base.IconTextSelectButton
import com.ismartcoding.plain.ui.base.IconTextShareButton
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.components.FileRenameDialog
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.FilesViewModel
import com.ismartcoding.plain.ui.models.enterSelectMode
import com.ismartcoding.plain.ui.models.select
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FileInfoBottomSheet(
    filesVM: FilesViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val file = filesVM.selectedFile.value ?: return
    var isFavorite by remember { mutableStateOf(false) }
    
    val onDismiss = {
        filesVM.selectedFile.value = null
    }

    LaunchedEffect(file.path) {
        if (file.isDir) {
            isFavorite = FavoriteFoldersPreference.isFavoriteAsync(context, file.path)
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

    PModalBottomSheet(
        onDismissRequest = {
            onDismiss()
        },
    ) {
        LazyColumn {
            item {
                VerticalSpace(32.dp)
            }
            item {
                ActionButtons {
                    if (!filesVM.showSearchBar.value) {
                        IconTextSelectButton {
                            filesVM.enterSelectMode()
                            filesVM.select(file.path)
                            onDismiss()
                        }
                    }

                    if (file.isDir) {
                        IconTextFavoriteButton(
                            isFavorite = isFavorite
                        ) {
                            scope.launch(Dispatchers.IO) {
                                if (isFavorite) {
                                    FavoriteFoldersPreference.removeAsync(context, file.path)
                                    isFavorite = false
                                } else {
                                    FavoriteFoldersPreference.addAsync(
                                        context,
                                        DFavoriteFolder(
                                            rootPath = filesVM.rootPath,
                                            fullPath = file.path
                                        )
                                    )
                                    isFavorite = true
                                }
                            }
                        }
                    }

                    IconTextShareButton {
                        ShareHelper.sharePaths(context, setOf(file.path))
                        onDismiss()
                    }
                    IconTextRenameButton {
                        filesVM.showRenameDialog.value = true
                    }
                    IconTextDeleteButton {
                        DialogHelper.confirmToDelete {
                            filesVM.deleteFiles(setOf(file.path))
                            onDismiss()
                        }
                    }
                }
                VerticalSpace(dp = 24.dp)
                PCard {
                    PListItem(title = file.path, action = {
                        PIconButton(icon = R.drawable.copy, contentDescription = stringResource(id = R.string.copy_path), click = {
                            val clip = ClipData.newPlainText(LocaleHelper.getString(R.string.file_path), file.path)
                            clipboardManager.setPrimaryClip(clip)
                            DialogHelper.showTextCopiedMessage(file.path)
                        })
                    })
                }
                VerticalSpace(dp = 16.dp)
                PCard {
                    PListItem(title = stringResource(id = R.string.file_size), value = file.size.formatBytes())
                    PListItem(title = stringResource(id = R.string.type), value = if (file.isDir) stringResource(id = R.string.folder) else file.path.getMimeType())
                    file.createdAt?.let {
                        PListItem(title = stringResource(id = R.string.created_at), value = it.formatDateTime())
                    }
                    PListItem(title = stringResource(id = R.string.updated_at), value = file.updatedAt.formatDateTime())
                    if (file.isDir && file.children > 0) {
                        PListItem(title = stringResource(id = R.string.items), value = file.children.toString())
                    }
                }
            }
            item {
                BottomSpace()
            }
        }
    }
} 