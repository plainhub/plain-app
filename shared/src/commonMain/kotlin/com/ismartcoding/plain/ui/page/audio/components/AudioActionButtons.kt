package com.ismartcoding.plain.ui.page.audio.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.ui.theme.PlainTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.ismartcoding.plain.lib.extensions.isUrl
import com.ismartcoding.plain.lib.extensions.getFilenameWithoutExtensionFromPath
import com.ismartcoding.plain.audio.DAudio
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.enums.has
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.platform.addMediaShortcut
import com.ismartcoding.plain.platform.getMediaItemUriString
import com.ismartcoding.plain.platform.openFileExternal
import com.ismartcoding.plain.platform.shareFiles
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PSheetActionRow
import com.ismartcoding.plain.ui.base.PSheetPrimaryAction
import com.ismartcoding.plain.ui.base.PSheetPrimaryActionsCard
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.components.AddToHomeDialog
import com.ismartcoding.plain.ui.base.dragselect.DragSelectState
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.AudioViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun AudioActionButtons(
    m: DAudio,
    audioVM: AudioViewModel,
    tagsVM: TagsViewModel,
    dragSelectState: DragSelectState,
    onDismiss: () -> Unit,
) {
    var showAddToHomeDialog by remember { mutableStateOf(false) }
    PSheetPrimaryActionsCard {
        if (!audioVM.trash.value) {
            PSheetPrimaryAction(Res.drawable.share_2, stringResource(Res.string.share)) {
                shareFiles(listOf(getMediaItemUriString(DataType.AUDIO, m.id)))
                onDismiss()
            }
            if (!m.path.isUrl()) {
                PSheetPrimaryAction(Res.drawable.square_arrow_out_up_right, stringResource(Res.string.open_with)) {
                    openFileExternal(m.path)
                }
            }
            PSheetPrimaryAction(Res.drawable.pen, stringResource(Res.string.rename)) {
                audioVM.showRenameDialog.value = true
            }
        }
        if (AppFeatureType.MEDIA_TRASH.has() && audioVM.trash.value) {
            PSheetPrimaryAction(Res.drawable.archive_restore, stringResource(Res.string.restore)) {
                audioVM.restore(tagsVM, setOf(m.id))
                onDismiss()
            }
        } else {
            PSheetPrimaryAction(
                Res.drawable.delete_forever,
                stringResource(Res.string.delete),
                container = MaterialTheme.colorScheme.errorContainer,
                tint = MaterialTheme.colorScheme.error,
            ) {
                DialogHelper.confirmToDelete {
                    audioVM.delete(tagsVM, setOf(m.id))
                    onDismiss()
                }
            }
        }
    }
    val hasSecondary = !audioVM.showSearchBar.value || (!audioVM.trash.value && !m.path.isUrl()) ||
        (AppFeatureType.MEDIA_TRASH.has() && !audioVM.trash.value)
    if (hasSecondary) {
        VerticalSpace(12.dp)
        PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
            Column {
                if (!audioVM.showSearchBar.value) {
                    PSheetActionRow(Res.drawable.list_checks, stringResource(Res.string.select)) {
                        dragSelectState.enterSelectMode()
                        dragSelectState.select(m.id)
                        onDismiss()
                    }
                }
                if (!audioVM.trash.value && !m.path.isUrl()) {
                    PSheetActionRow(Res.drawable.smartphone, stringResource(Res.string.add_to_home)) {
                        showAddToHomeDialog = true
                    }
                }
                if (AppFeatureType.MEDIA_TRASH.has() && !audioVM.trash.value) {
                    PSheetActionRow(Res.drawable.trash_2, stringResource(Res.string.trash)) {
                        audioVM.trash(tagsVM, setOf(m.id))
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
