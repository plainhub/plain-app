package com.ismartcoding.plain.ui.files

import com.ismartcoding.plain.rust.graphql.ContentSource
import com.ismartcoding.plain.platform.permissions.supportsDirectorySelection
import com.ismartcoding.plain.features.web.AccessFeature
import com.ismartcoding.plain.platform.permissions.requestSystemAccess

import com.ismartcoding.plain.preferences.*

import com.ismartcoding.plain.i18n.*

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.features.files.ZipBrowserHelper
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import com.ismartcoding.plain.ui.base.PSheetActionCard
import com.ismartcoding.plain.ui.base.PSheetActionRow
import androidx.compose.material3.Switch
import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.state.enterSelectMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.eye as ui_drawable_eye
import com.ismartcoding.plain.ui.resources.eye_off as ui_drawable_eye_off
import com.ismartcoding.plain.ui.resources.file_plus as ui_drawable_file_plus
import com.ismartcoding.plain.ui.resources.folder_plus as ui_drawable_folder_plus
import com.ismartcoding.plain.ui.resources.list_checks as ui_drawable_list_checks
import com.ismartcoding.plain.ui.resources.sort as ui_drawable_sort
import com.ismartcoding.plain.i18n.sort

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesMoreActionsSheet(filesVM: FilesViewModel, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    val showHiddenFiles by Prefs.showHiddenFiles.collectAsState()
    val isZip = ZipBrowserHelper.isZipPath(filesVM.selectedPath)

    PModalBottomSheet(onDismissRequest = onDismiss) {
        Column {
            VerticalSpace(16.dp)
            PSheetActionCard {
                if (!isZip) {
                    PSheetActionRow(UiRes.drawable.ui_drawable_list_checks, stringResource(Res.string.select)) {
                        onDismiss()
                        filesVM.enterSelectMode()
                    }
                }
                PSheetActionRow(UiRes.drawable.ui_drawable_sort, stringResource(Res.string.sort)) {
                    onDismiss()
                    filesVM.showSortDialog.value = true
                }
                PListItem(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            onDismiss()
                            scope.launch(Dispatchers.Default) {
                                Prefs.set(PrefKeys.SHOW_HIDDEN_FILES, !showHiddenFiles)
                            }
                        },
                    icon = if (showHiddenFiles) UiRes.drawable.ui_drawable_eye else UiRes.drawable.ui_drawable_eye_off,
                    title = stringResource(Res.string.show_hidden_files),
                    action = {
                        Switch(checked = showHiddenFiles, onCheckedChange = { enabled ->
                            onDismiss()
                            scope.launch(Dispatchers.Default) {
                                Prefs.set(PrefKeys.SHOW_HIDDEN_FILES, enabled)
                            }
                        })
                    },
                )
            }
            if (!isZip) {
                VerticalSpace(16.dp)
                PSheetActionCard {
                    if (filesVM.source == ContentSource.Local && supportsDirectorySelection) {
                        PSheetActionRow(UiRes.drawable.ui_drawable_folder_plus, stringResource(Res.string.add_folder)) {
                            onDismiss()
                            scope.launch { setOf(AccessFeature.FILES).requestSystemAccess() }
                        }
                    }
                    PSheetActionRow(UiRes.drawable.ui_drawable_folder_plus, stringResource(Res.string.create_folder)) {
                        onDismiss()
                        filesVM.showCreateFolderDialog.value = true
                    }
                    PSheetActionRow(UiRes.drawable.ui_drawable_file_plus, stringResource(Res.string.create_file)) {
                        onDismiss()
                        filesVM.showCreateFileDialog.value = true
                    }
                }
            }
            BottomSpace()
        }
    }
}
