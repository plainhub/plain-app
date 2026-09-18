package com.ismartcoding.plain.ui.page.docs
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import com.ismartcoding.plain.ui.theme.PlainTheme

import com.ismartcoding.plain.i18n.*

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.lib.extensions.formatBytes
import com.ismartcoding.plain.lib.extensions.getFilenameFromPath
import com.ismartcoding.plain.lib.extensions.getMimeType
import com.ismartcoding.plain.platform.isRPlus
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.db.DTagRelation
import com.ismartcoding.plain.platform.formatDateTime
import com.ismartcoding.plain.platform.openFileExternal
import com.ismartcoding.plain.platform.shareFile
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.enums.has
import com.ismartcoding.plain.ui.base.CopyIconButton
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import com.ismartcoding.plain.ui.base.PSheetActionRow
import com.ismartcoding.plain.ui.base.PSheetPrimaryAction
import com.ismartcoding.plain.ui.base.PSheetPrimaryActionsCard
import com.ismartcoding.plain.ui.base.Subtitle
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.dragselect.DragSelectState
import com.ismartcoding.plain.ui.components.FileRenameDialog
import com.ismartcoding.plain.platform.renameAndScanFile
import com.ismartcoding.plain.ui.components.TagSelector
import com.ismartcoding.plain.ui.helpers.confirmActionAsync
import com.ismartcoding.plain.ui.models.DocsViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ViewDocBottomSheet(
    docsVM: DocsViewModel,
    tagsVM: TagsViewModel,
    tagsMapState: Map<String, List<DTagRelation>>,
    tagsState: List<DTag>,
    dragSelectState: DragSelectState,
) {
    val scope = rememberCoroutineScope()
    val m = docsVM.selectedItem.value ?: return
    val onDismiss = {
        docsVM.selectedItem.value = null
    }

    if (docsVM.showRenameDialog.value) {
        FileRenameDialog(path = m.path, onDismiss = {
            docsVM.showRenameDialog.value = false
        }, onRename = { p, name -> renameAndScanFile(p, name) }, onRenamed = {
            m.path = it
            m.title = it.getFilenameFromPath()
        })
    }

    PModalBottomSheet(
        onDismissRequest = {
            onDismiss()
        },
    ) {
        LazyColumn {
            item {
                VerticalSpace(16.dp)
            }
            item {
                PSheetPrimaryActionsCard {
                    if (!docsVM.showSearchBar.value) {
                        PSheetPrimaryAction(Res.drawable.list_checks, stringResource(Res.string.select)) {
                            dragSelectState.enterSelectMode()
                            dragSelectState.select(m.id)
                            onDismiss()
                        }
                    }
                    if (!docsVM.trash.value) {
                        PSheetPrimaryAction(Res.drawable.share_2, stringResource(Res.string.share)) {
                            shareFile(m.path)
                            onDismiss()
                        }
                        PSheetPrimaryAction(Res.drawable.square_arrow_out_up_right, stringResource(Res.string.open_with)) {
                            openFileExternal(m.path)
                        }
                        // At most 4 disc actions per card: rename drops to the
                        // secondary rows when delete occupies the fourth slot
                        // (no-trash builds).
                        if (AppFeatureType.MEDIA_TRASH.has()) {
                            PSheetPrimaryAction(Res.drawable.pen, stringResource(Res.string.rename)) {
                                docsVM.showRenameDialog.value = true
                            }
                        }
                    }
                    if (AppFeatureType.MEDIA_TRASH.has() && docsVM.trash.value) {
                        PSheetPrimaryAction(Res.drawable.archive_restore, stringResource(Res.string.restore)) {
                            if (isRPlus()) {
                                docsVM.restore(tagsVM, setOf(m.id))
                                onDismiss()
                            }
                        }
                    }
                    if (!AppFeatureType.MEDIA_TRASH.has() || docsVM.trash.value) {
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
                                        docsVM.delete(tagsVM, setOf(m.id))
                                        onDismiss()
                                    },
                                    danger = true
                                )
                            }
                        }
                    }
                }
                val hasSecondary = !docsVM.trash.value
                if (hasSecondary) {
                    VerticalSpace(12.dp)
                    PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
                        Column {
                            if (AppFeatureType.MEDIA_TRASH.has() && !docsVM.trash.value) {
                                PSheetActionRow(Res.drawable.trash_2, stringResource(Res.string.trash)) {
                                    if (isRPlus()) {
                                        docsVM.trash(tagsVM, setOf(m.id))
                                    }
                                }
                            }
                            if (!AppFeatureType.MEDIA_TRASH.has() && !docsVM.trash.value) {
                                PSheetActionRow(Res.drawable.pen, stringResource(Res.string.rename)) {
                                    docsVM.showRenameDialog.value = true
                                }
                            }
                        }
                    }
                }
                VerticalSpace(dp = 24.dp)
                if (!docsVM.trash.value) {
                    Subtitle(text = stringResource(Res.string.tags))
                    TagSelector(
                        data = m,
                        tagsVM = tagsVM,
                        tagsMap = tagsMapState,
                        tagsState = tagsState,
                        onChangedAsync = { docsVM.loadAsync(tagsVM) }
                    )
                    VerticalSpace(dp = 16.dp)
                }
                PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
                    PListItem(title = m.path, action = {
                        CopyIconButton(text = m.path, clipLabel = stringResource(Res.string.file_path))
                    })
                }
                VerticalSpace(dp = 16.dp)
                PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
                    PListItem(title = stringResource(Res.string.file_size), value = m.size.formatBytes())
                    PListItem(title = stringResource(Res.string.type), value = m.path.getMimeType())
                    PListItem(title = stringResource(Res.string.created_at), value = m.createdAt.formatDateTime())
                    PListItem(title = stringResource(Res.string.updated_at), value = m.updatedAt.formatDateTime())
                }
            }
            item {
                BottomSpace()
            }
        }
    }
}
