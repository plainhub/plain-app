package com.ismartcoding.plain.ui.files.documents


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ismartcoding.plain.platform.system.AppFeatureType
import com.ismartcoding.plain.platform.system.has
import com.ismartcoding.plain.models.Tag
import com.ismartcoding.plain.models.TagRelation
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.lib.extensions.formatBytes
import com.ismartcoding.plain.lib.extensions.getFilenameFromPath
import com.ismartcoding.plain.lib.extensions.getFilenameWithoutExtensionFromPath
import com.ismartcoding.plain.lib.extensions.getMimeType
import com.ismartcoding.plain.ui.app.MainViewModel
import com.ismartcoding.plain.platform.system.Capability
import com.ismartcoding.plain.platform.system.getDeviceCapabilities
import com.ismartcoding.plain.platform.system.formatDateTime
import com.ismartcoding.plain.ui.media.fileIconData
import com.ismartcoding.plain.platform.system.openFileExternal
import com.ismartcoding.plain.platform.system.shareFile
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.CopyIconButton
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import com.ismartcoding.plain.ui.base.PSheetActionRow
import com.ismartcoding.plain.ui.base.PSheetHeader
import com.ismartcoding.plain.ui.base.PSheetHeaderThumb
import com.ismartcoding.plain.ui.base.PSheetPrimaryAction
import com.ismartcoding.plain.ui.base.PSheetPrimaryActionsRow
import com.ismartcoding.plain.ui.base.PSheetPrimaryDeleteAction
import com.ismartcoding.plain.ui.base.PSheetPrimaryTrashAction
import com.ismartcoding.plain.ui.base.PSectionTitle
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.dragselect.DragSelectState
import com.ismartcoding.plain.ui.components.AddToHomeDialog
import com.ismartcoding.plain.ui.components.AddToHomeHelpAction
import com.ismartcoding.plain.ui.components.FileRenameDialog
import com.ismartcoding.plain.ui.components.TagSelector
import com.ismartcoding.plain.ui.components.confirmActionAsync
import com.ismartcoding.plain.ui.tags.TagsViewModel
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.archive_restore as ui_drawable_archive_restore
import com.ismartcoding.plain.ui.resources.file as ui_drawable_file
import com.ismartcoding.plain.ui.resources.list_checks as ui_drawable_list_checks
import com.ismartcoding.plain.ui.resources.pen as ui_drawable_pen
import com.ismartcoding.plain.ui.resources.share_2 as ui_drawable_share_2
import com.ismartcoding.plain.ui.resources.square_arrow_out_up_right as ui_drawable_square_arrow_out_up_right
import com.ismartcoding.plain.ui.theme.PlainTheme
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ViewDocBottomSheet(
    docsVM: DocsViewModel,
    tagsVM: TagsViewModel,
    tagsMapState: Map<String, List<TagRelation>>,
    tagsState: List<Tag>,
    dragSelectState: DragSelectState,
) {
    val scope = rememberCoroutineScope()
    var showAddToHomeDialog by remember { mutableStateOf(false) }
    val canAddToHome = docsVM.source is com.ismartcoding.plain.rust.graphql.ContentSource.Local && Capability.LAUNCHER_SHORTCUTS in getDeviceCapabilities()
    val m = docsVM.selectedItem.value ?: return
    val onDismiss = {
        docsVM.selectedItem.value = null
    }

    if (showAddToHomeDialog && canAddToHome) {
        AddToHomeDialog(
            defaultLabel = remember(m.path) { m.path.getFilenameWithoutExtensionFromPath() },
            onAddToHome = { label -> MainViewModel.addMediaShortcut(m.path, label) },
            onDismiss = {
                showAddToHomeDialog = false
                onDismiss()
            })
    }

    if (docsVM.showRenameDialog.value) {
        FileRenameDialog(path = m.path, onDismiss = {
            docsVM.showRenameDialog.value = false
        }, onRename = { p, name -> docsVM.source.files.rename(p, name) }, onRenamed = {
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
                PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
                    PSheetHeader(
                        thumbnail = {
                            PSheetHeaderThumb(
                                model = fileIconData(m.extension),
                                fallbackIcon = UiRes.drawable.ui_drawable_file,
                            )
                        },
                        title = m.title.ifEmpty { m.path.getFilenameFromPath() },
                        subtitle = m.path.getMimeType() + " · " + m.size.formatBytes(),
                    )
                    PSheetPrimaryActionsRow {
                        if (!docsVM.showSearchBar.value) {
                            PSheetPrimaryAction(UiRes.drawable.ui_drawable_list_checks, stringResource(Res.string.select)) {
                                dragSelectState.enterSelectMode()
                                dragSelectState.select(m.id)
                                onDismiss()
                            }
                        }
                        if (!docsVM.trash.value) {
                            PSheetPrimaryAction(UiRes.drawable.ui_drawable_share_2, stringResource(Res.string.share)) {
                                shareFile(m.path)
                                onDismiss()
                            }
                            PSheetPrimaryAction(UiRes.drawable.ui_drawable_pen, stringResource(Res.string.rename)) {
                                docsVM.showRenameDialog.value = true
                            }
                            if (AppFeatureType.MEDIA_TRASH.has(docsVM.source.capabilities)) {
                                PSheetPrimaryTrashAction(stringResource(Res.string.trash)) {
                                    docsVM.trash(tagsVM, setOf(m.id))
                                    onDismiss()
                                }
                            }
                        }
                        if (AppFeatureType.MEDIA_TRASH.has(docsVM.source.capabilities) && docsVM.trash.value) {
                            PSheetPrimaryAction(UiRes.drawable.ui_drawable_archive_restore, stringResource(Res.string.restore)) {
                                docsVM.restore(tagsVM, setOf(m.id))
                                onDismiss()
                            }
                        }
                        if (!AppFeatureType.MEDIA_TRASH.has(docsVM.source.capabilities) || docsVM.trash.value) {
                            PSheetPrimaryDeleteAction(stringResource(Res.string.delete)) {
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
                }
                if (!docsVM.trash.value) {
                    VerticalSpace(12.dp)
                    PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
                        Column {
                            if (canAddToHome) {
                                PSheetActionRow(
                                    title = stringResource(Res.string.add_to_home),
                                    start = {
                                        AsyncImage(
                                            model = fileIconData(m.extension),
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp),
                                        )
                                    },
                                    trailing = { AddToHomeHelpAction() },
                                ) {
                                    showAddToHomeDialog = true
                                }
                            }
                            PSheetActionRow(UiRes.drawable.ui_drawable_square_arrow_out_up_right, stringResource(Res.string.open_with)) {
                                docsVM.openFile(m.path)
                            }
                        }
                    }
                }
                VerticalSpace(dp = 24.dp)
                if (!docsVM.trash.value) {
                    PSectionTitle(title = stringResource(Res.string.tags))
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
