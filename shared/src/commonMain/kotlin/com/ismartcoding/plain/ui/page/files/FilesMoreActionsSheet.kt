package com.ismartcoding.plain.ui.page.files

import com.ismartcoding.plain.i18n.*

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.features.file.ZipBrowserHelper
import com.ismartcoding.plain.preferences.ShowHiddenFilesPreference
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import com.ismartcoding.plain.ui.base.PSheetActionCard
import com.ismartcoding.plain.ui.base.PSheetActionRow
import com.ismartcoding.plain.ui.base.PSwitch
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.models.FilesViewModel
import com.ismartcoding.plain.ui.models.enterSelectMode
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
    var showHiddenFiles by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        showHiddenFiles = ShowHiddenFilesPreference.getAsync()
    }
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
                                ShowHiddenFilesPreference.putAsync(!showHiddenFiles)
                                filesVM.loadAsync()
                            }
                        },
                    icon = if (showHiddenFiles) UiRes.drawable.ui_drawable_eye else UiRes.drawable.ui_drawable_eye_off,
                    title = stringResource(Res.string.show_hidden_files),
                    action = {
                        PSwitch(activated = showHiddenFiles, onClick = {
                            onDismiss()
                            scope.launch(Dispatchers.Default) {
                                ShowHiddenFilesPreference.putAsync(!showHiddenFiles)
                                filesVM.loadAsync()
                            }
                        })
                    },
                )
            }
            if (!isZip) {
                VerticalSpace(16.dp)
                PSheetActionCard {
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
