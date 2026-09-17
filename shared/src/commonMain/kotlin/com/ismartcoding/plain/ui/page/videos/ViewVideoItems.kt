package com.ismartcoding.plain.ui.page.videos
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import com.ismartcoding.plain.ui.theme.PlainTheme

import com.ismartcoding.plain.i18n.*

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.lib.extensions.getFilenameWithoutExtensionFromPath
import com.ismartcoding.plain.lib.extensions.isUrl
import com.ismartcoding.plain.data.DVideo
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.enums.has
import com.ismartcoding.plain.platform.addMediaShortcut
import com.ismartcoding.plain.platform.getMediaItemUriString
import com.ismartcoding.plain.platform.openFileExternal
import com.ismartcoding.plain.platform.shareFiles
import com.ismartcoding.plain.ui.base.CopyIconButton
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PSheetActionRow
import com.ismartcoding.plain.ui.base.PSheetPrimaryAction
import com.ismartcoding.plain.ui.base.PSheetPrimaryActionsCard
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.dragselect.DragSelectState
import com.ismartcoding.plain.ui.components.AddToHomeDialog
import com.ismartcoding.plain.ui.components.AddToHomeHelpAction
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.models.VideosViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun VideoActionButtons(
    m: DVideo,
    videosVM: VideosViewModel,
    tagsVM: TagsViewModel,
    dragSelectState: DragSelectState,
    onDismiss: () -> Unit,
) {
    var showAddToHomeDialog by remember { mutableStateOf(false) }
    PSheetPrimaryActionsCard {
        if (!videosVM.showSearchBar.value) {
            PSheetPrimaryAction(Res.drawable.list_checks, stringResource(Res.string.select)) {
                dragSelectState.enterSelectMode()
                dragSelectState.select(m.id)
                onDismiss()
            }
        }
        PSheetPrimaryAction(Res.drawable.share_2, stringResource(Res.string.share)) {
            shareFiles(listOf(getMediaItemUriString(videosVM.dataType, m.id)))
            onDismiss()
        }
        if (!m.path.isUrl()) {
            PSheetPrimaryAction(Res.drawable.square_arrow_out_up_right, stringResource(Res.string.open_with)) {
                openFileExternal(m.path)
            }
        }
        PSheetPrimaryAction(Res.drawable.pen, stringResource(Res.string.rename)) {
            videosVM.showRenameDialog.value = true
        }
        if (AppFeatureType.MEDIA_TRASH.has() && videosVM.trash.value) {
            PSheetPrimaryAction(Res.drawable.archive_restore, stringResource(Res.string.restore)) {
                videosVM.restore(tagsVM, setOf(m.id))
                onDismiss()
            }
        }
        if (!AppFeatureType.MEDIA_TRASH.has() || videosVM.trash.value) {
            PSheetPrimaryAction(
                Res.drawable.delete_forever,
                stringResource(Res.string.delete),
                container = MaterialTheme.colorScheme.errorContainer,
                tint = MaterialTheme.colorScheme.error,
            ) {
                DialogHelper.confirmToDelete {
                    videosVM.delete(tagsVM, setOf(m.id))
                    onDismiss()
                }
            }
        }
    }
    val hasSecondary = (!m.path.isUrl() && !videosVM.trash.value) ||
        (AppFeatureType.MEDIA_TRASH.has() && !videosVM.trash.value)
    if (hasSecondary) {
        VerticalSpace(12.dp)
        PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
            Column {
                if (!m.path.isUrl() && !videosVM.trash.value) {
                    PSheetActionRow(Res.drawable.smartphone, stringResource(Res.string.add_to_home), trailing = { AddToHomeHelpAction() }) {
                        showAddToHomeDialog = true
                    }
                }
                if (AppFeatureType.MEDIA_TRASH.has() && !videosVM.trash.value) {
                    PSheetActionRow(Res.drawable.trash_2, stringResource(Res.string.trash)) {
                        videosVM.trash(tagsVM, setOf(m.id))
                        onDismiss()
                    }
                }
            }
        }
    }
    if (showAddToHomeDialog) {
        AddToHomeDialog(
            defaultLabel = remember(m.path) { m.path.getFilenameWithoutExtensionFromPath() },
            onAddToHome = { label -> addMediaShortcut(m.path, label) },
            onDismiss = {
                showAddToHomeDialog = false
                onDismiss()
            })
    }
}

@Composable
internal fun VideoPathCard(m: DVideo) {
    PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
        PListItem(title = m.path, action = {
            CopyIconButton(text = m.path, clipLabel = stringResource(Res.string.file_path))
        })
    }
}
