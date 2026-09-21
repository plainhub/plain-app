package com.ismartcoding.plain.ui.page.audio.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.ui.theme.PlainTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
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
import com.ismartcoding.plain.features.audio.AudioQueueManager
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.platform.addMediaShortcut
import com.ismartcoding.plain.platform.getMediaItemUriString
import com.ismartcoding.plain.platform.openFileExternal
import com.ismartcoding.plain.platform.shareFiles
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PSheetActionRow
import com.ismartcoding.plain.ui.base.PSheetPrimaryAction
import com.ismartcoding.plain.ui.base.PSheetPrimaryTrashAction
import com.ismartcoding.plain.ui.base.PSheetPrimaryDeleteAction
import com.ismartcoding.plain.ui.base.PSheetHeader
import com.ismartcoding.plain.ui.base.PSheetPrimaryActionsRow
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.components.AddToHomeDialog
import com.ismartcoding.plain.ui.components.AddToHomeHelpAction
import com.ismartcoding.plain.ui.base.dragselect.DragSelectState
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.helpers.confirmActionAsync
import com.ismartcoding.plain.ui.models.AudioViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.layout.size
import com.ismartcoding.plain.lib.extensions.formatDuration
import com.ismartcoding.plain.lib.extensions.getFilenameFromPath

@Composable
internal fun AudioActionButtons(
    m: DAudio,
    audioVM: AudioViewModel,
    tagsVM: TagsViewModel,
    dragSelectState: DragSelectState,
    onDismiss: () -> Unit,
    playlistId: String? = null,
    onPlaylistChanged: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    var showAddToHomeDialog by remember { mutableStateOf(false) }
    PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
        PSheetHeader(
            thumbnail = {
                AudioCoverOrIcon(path = m.path, modifier = Modifier.size(44.dp))
            },
            title = m.title.ifEmpty { m.path.getFilenameFromPath() },
            subtitle = (if (m.artist.isNotEmpty()) m.artist + " · " else "") + m.duration.formatDuration(),
        )
        PSheetPrimaryActionsRow {
            if (!audioVM.showSearchBar.value) {
                PSheetPrimaryAction(Res.drawable.list_checks, stringResource(Res.string.select)) {
                    dragSelectState.enterSelectMode()
                    dragSelectState.select(m.id)
                    onDismiss()
                }
            }
            if (!audioVM.trash.value) {
                PSheetPrimaryAction(Res.drawable.share_2, stringResource(Res.string.share)) {
                    shareFiles(listOf(getMediaItemUriString(DataType.AUDIO, m.id)))
                    onDismiss()
                }
                PSheetPrimaryAction(Res.drawable.pen, stringResource(Res.string.rename)) {
                    audioVM.showRenameDialog.value = true
                }
                if (AppFeatureType.MEDIA_TRASH.has()) {
                    PSheetPrimaryTrashAction(stringResource(Res.string.trash)) {
                        audioVM.trash(tagsVM, setOf(m.id))
                        onDismiss()
                    }
                }
            }
            if (AppFeatureType.MEDIA_TRASH.has() && audioVM.trash.value) {
                PSheetPrimaryAction(Res.drawable.archive_restore, stringResource(Res.string.restore)) {
                    audioVM.restore(tagsVM, setOf(m.id))
                    onDismiss()
                }
            }
            if (!AppFeatureType.MEDIA_TRASH.has() || audioVM.trash.value) {
                PSheetPrimaryDeleteAction(stringResource(Res.string.delete)) {
                    scope.launch {
                        confirmActionAsync(
                            Res.string.delete,
                            Res.string.confirm_to_delete,
                            callback = {
                                audioVM.delete(tagsVM, setOf(m.id))
                                onDismiss()
                            },
                            danger = true
                        )
                    }
                }
            }
        }
    }
    val hasSecondary = (!audioVM.trash.value && !m.path.isUrl()) || playlistId != null
    if (hasSecondary) {
        VerticalSpace(12.dp)
        PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
            Column {
                if (playlistId != null) {
                    PSheetActionRow(Res.drawable.playlist_remove, stringResource(Res.string.remove_from_playlist)) {
                        scope.launch {
                            withIO { AudioQueueManager.removePlaylistItem(playlistId, m.path) }
                            DialogHelper.showMessage(Res.string.removed_from_playlist)
                            onPlaylistChanged()
                            onDismiss()
                        }
                    }
                }
                if (!audioVM.trash.value && !m.path.isUrl()) {
                    PSheetActionRow(Res.drawable.smartphone, stringResource(Res.string.add_to_home), trailing = { AddToHomeHelpAction() }) {
                        showAddToHomeDialog = true
                    }
                    PSheetActionRow(Res.drawable.square_arrow_out_up_right, stringResource(Res.string.open_with)) {
                        openFileExternal(m.path)
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
