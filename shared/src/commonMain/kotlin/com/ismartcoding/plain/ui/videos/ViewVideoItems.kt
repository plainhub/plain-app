package com.ismartcoding.plain.ui.videos

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import com.ismartcoding.plain.models.Video as MediaVideo
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import com.ismartcoding.plain.ui.theme.PlainTheme
import com.ismartcoding.plain.i18n.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.lib.extensions.getFilenameWithoutExtensionFromPath
import com.ismartcoding.plain.lib.extensions.isUrl
import com.ismartcoding.plain.platform.system.AppFeatureType
import com.ismartcoding.plain.platform.system.has
import com.ismartcoding.plain.ui.app.MainViewModel
import com.ismartcoding.plain.platform.system.Capability
import com.ismartcoding.plain.platform.system.getDeviceCapabilities
import com.ismartcoding.plain.platform.media.getMediaItemUriString
import com.ismartcoding.plain.platform.system.openFileExternal
import com.ismartcoding.plain.platform.system.shareFiles
import com.ismartcoding.plain.ui.base.CopyIconButton
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PSheetActionRow
import com.ismartcoding.plain.ui.base.PSheetPrimaryAction
import com.ismartcoding.plain.ui.base.PSheetPrimaryTrashAction
import com.ismartcoding.plain.ui.base.PSheetPrimaryDeleteAction
import com.ismartcoding.plain.ui.base.PSheetHeader
import com.ismartcoding.plain.ui.base.PSheetHeaderThumb
import com.ismartcoding.plain.ui.base.PSheetPrimaryActionsRow
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.dragselect.DragSelectState
import com.ismartcoding.plain.ui.components.AddToHomeDialog
import com.ismartcoding.plain.ui.components.AddToHomeHelpAction
import com.ismartcoding.plain.ui.components.confirmActionAsync
import com.ismartcoding.plain.ui.tags.TagsViewModel
import androidx.compose.ui.layout.ContentScale
import com.ismartcoding.plain.lib.extensions.formatBytes
import com.ismartcoding.plain.lib.extensions.getFilenameFromPath
import com.ismartcoding.plain.lib.extensions.getMimeType
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.archive_restore as ui_drawable_archive_restore
import com.ismartcoding.plain.ui.resources.file_video as ui_drawable_file_video
import com.ismartcoding.plain.ui.resources.list_checks as ui_drawable_list_checks
import com.ismartcoding.plain.ui.resources.pen as ui_drawable_pen
import com.ismartcoding.plain.ui.resources.share_2 as ui_drawable_share_2
import com.ismartcoding.plain.ui.resources.smartphone as ui_drawable_smartphone
import com.ismartcoding.plain.ui.resources.square_arrow_out_up_right as ui_drawable_square_arrow_out_up_right

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun VideoActionButtons(
    m: MediaVideo,
    videosVM: VideosViewModel,
    tagsVM: TagsViewModel,
    dragSelectState: DragSelectState,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var showAddToHomeDialog by remember { mutableStateOf(false) }
    val canAddToHome = videosVM.source is com.ismartcoding.plain.rust.graphql.ContentSource.Local && Capability.LAUNCHER_SHORTCUTS in getDeviceCapabilities()
    PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
        PSheetHeader(
            thumbnail = {
                PSheetHeaderThumb(
                    model = getMediaItemUriString(videosVM.dataType, m.id, m.path),
                    fallbackIcon = UiRes.drawable.ui_drawable_file_video,
                    contentScale = ContentScale.Crop,
                )
            },
            title = m.title.ifEmpty { m.path.getFilenameFromPath() },
            subtitle = m.path.getMimeType() + " · " + m.size.formatBytes(),
        )
        PSheetPrimaryActionsRow {
            if (!videosVM.showSearchBar.value) {
                PSheetPrimaryAction(UiRes.drawable.ui_drawable_list_checks, stringResource(Res.string.select)) {
                    dragSelectState.enterSelectMode()
                    dragSelectState.select(m.id)
                    onDismiss()
                }
            }
            PSheetPrimaryAction(UiRes.drawable.ui_drawable_share_2, stringResource(Res.string.share)) {
                videosVM.share(listOf(m.id))
                onDismiss()
            }
            if (!videosVM.trash.value) {
                PSheetPrimaryAction(UiRes.drawable.ui_drawable_pen, stringResource(Res.string.rename)) {
                    videosVM.showRenameDialog.value = true
                }
            }
            if (AppFeatureType.MEDIA_TRASH.has(videosVM.source.capabilities) && !videosVM.trash.value) {
                PSheetPrimaryTrashAction(stringResource(Res.string.trash)) {
                    videosVM.trash(tagsVM, setOf(m.id))
                    onDismiss()
                }
            }
            if (AppFeatureType.MEDIA_TRASH.has(videosVM.source.capabilities) && videosVM.trash.value) {
                PSheetPrimaryAction(UiRes.drawable.ui_drawable_archive_restore, stringResource(Res.string.restore)) {
                    videosVM.restore(tagsVM, setOf(m.id))
                    onDismiss()
                }
            }
            if (!AppFeatureType.MEDIA_TRASH.has(videosVM.source.capabilities) || videosVM.trash.value) {
                PSheetPrimaryDeleteAction(stringResource(Res.string.delete)) {
                    scope.launch {
                        confirmActionAsync(
                            Res.string.delete,
                            Res.string.confirm_to_delete,
                            callback = {
                                videosVM.delete(tagsVM, setOf(m.id))
                                onDismiss()
                            },
                            danger = true
                        )
                    }
                }
            }
        }
    }
    val hasSecondary = (!m.path.isUrl() && !videosVM.trash.value) ||
        videosVM.trash.value
    if (hasSecondary) {
        VerticalSpace(12.dp)
        PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
            Column {
                if (!m.path.isUrl() && !videosVM.trash.value) {
                    if (canAddToHome) {
                        PSheetActionRow(UiRes.drawable.ui_drawable_smartphone, stringResource(Res.string.add_to_home), trailing = { AddToHomeHelpAction() }) {
                            showAddToHomeDialog = true
                        }
                    }
                    PSheetActionRow(UiRes.drawable.ui_drawable_square_arrow_out_up_right, stringResource(Res.string.open_with)) {
                        videosVM.openFile(m.path)
                    }
                }
                if (videosVM.trash.value) {
                    PSheetActionRow(UiRes.drawable.ui_drawable_pen, stringResource(Res.string.rename)) {
                        videosVM.showRenameDialog.value = true
                    }
                }
            }
        }
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
}

@Composable
internal fun VideoPathCard(m: MediaVideo) {
    PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
        PListItem(title = m.path, action = {
            CopyIconButton(text = m.path, clipLabel = stringResource(Res.string.file_path))
        })
    }
}
