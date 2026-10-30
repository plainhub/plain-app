package com.ismartcoding.plain.ui.audio.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import com.ismartcoding.plain.models.Audio as AudioTrack
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.ui.theme.PlainTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.ismartcoding.plain.lib.extensions.isUrl
import com.ismartcoding.plain.lib.extensions.getFilenameWithoutExtensionFromPath
import com.ismartcoding.plain.platform.system.AppFeatureType
import com.ismartcoding.plain.platform.system.has
import com.ismartcoding.plain.platform.system.DataType
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.ui.app.MainViewModel
import com.ismartcoding.plain.platform.system.Capability
import com.ismartcoding.plain.platform.system.getDeviceCapabilities
import com.ismartcoding.plain.platform.media.getMediaItemUriString
import com.ismartcoding.plain.platform.system.openFileExternal
import com.ismartcoding.plain.platform.system.shareFiles
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
import com.ismartcoding.plain.ui.components.DialogHelper
import com.ismartcoding.plain.ui.components.confirmActionAsync
import com.ismartcoding.plain.ui.audio.AudioViewModel
import com.ismartcoding.plain.ui.tags.TagsViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.layout.size
import com.ismartcoding.plain.lib.extensions.formatDurationMs
import com.ismartcoding.plain.lib.extensions.getFilenameFromPath
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.archive_restore as ui_drawable_archive_restore
import com.ismartcoding.plain.ui.resources.list_checks as ui_drawable_list_checks
import com.ismartcoding.plain.ui.resources.pen as ui_drawable_pen
import com.ismartcoding.plain.ui.resources.playlist_remove as ui_drawable_playlist_remove
import com.ismartcoding.plain.ui.resources.share_2 as ui_drawable_share_2
import com.ismartcoding.plain.ui.resources.smartphone as ui_drawable_smartphone
import com.ismartcoding.plain.ui.resources.square_arrow_out_up_right as ui_drawable_square_arrow_out_up_right

@Composable
internal fun AudioActionButtons(
    m: AudioTrack,
    audioVM: AudioViewModel,
    tagsVM: TagsViewModel,
    dragSelectState: DragSelectState,
    onDismiss: () -> Unit,
    playlistId: String? = null,
    onPlaylistChanged: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    var showAddToHomeDialog by remember { mutableStateOf(false) }
    val canAddToHome = audioVM.source is com.ismartcoding.plain.rust.graphql.ContentSource.Local && Capability.LAUNCHER_SHORTCUTS in getDeviceCapabilities()
    PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
        PSheetHeader(
            thumbnail = {
                AudioCoverOrIcon(path = m.path, modifier = Modifier.size(44.dp))
            },
            title = m.title.ifEmpty { m.path.getFilenameFromPath() },
            subtitle = (if (m.artist.isNotEmpty()) m.artist + " · " else "") + m.durationMs.formatDurationMs(),
        )
        PSheetPrimaryActionsRow {
            if (!audioVM.showSearchBar.value) {
                PSheetPrimaryAction(UiRes.drawable.ui_drawable_list_checks, stringResource(Res.string.select)) {
                    dragSelectState.enterSelectMode()
                    dragSelectState.select(m.id)
                    onDismiss()
                }
            }
            if (!audioVM.trash.value) {
                PSheetPrimaryAction(UiRes.drawable.ui_drawable_share_2, stringResource(Res.string.share)) {
                    audioVM.share(listOf(m.id))
                    onDismiss()
                }
                PSheetPrimaryAction(UiRes.drawable.ui_drawable_pen, stringResource(Res.string.rename)) {
                    audioVM.showRenameDialog.value = true
                }
                if (AppFeatureType.MEDIA_TRASH.has(audioVM.source.capabilities)) {
                    PSheetPrimaryTrashAction(stringResource(Res.string.trash)) {
                        audioVM.trash(tagsVM, setOf(m.id))
                        onDismiss()
                    }
                }
            }
            if (AppFeatureType.MEDIA_TRASH.has(audioVM.source.capabilities) && audioVM.trash.value) {
                PSheetPrimaryAction(UiRes.drawable.ui_drawable_archive_restore, stringResource(Res.string.restore)) {
                    audioVM.restore(tagsVM, setOf(m.id))
                    onDismiss()
                }
            }
            if (!AppFeatureType.MEDIA_TRASH.has(audioVM.source.capabilities) || audioVM.trash.value) {
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
                    PSheetActionRow(UiRes.drawable.ui_drawable_playlist_remove, stringResource(Res.string.remove_from_playlist)) {
                        scope.launch {
                            withIO { audioVM.source.playlists.removePlaylistItem(playlistId, m.id) }
                            DialogHelper.showMessage(Res.string.removed_from_playlist)
                            onPlaylistChanged()
                            onDismiss()
                        }
                    }
                }
                if (!audioVM.trash.value && !m.path.isUrl()) {
                    if (canAddToHome) {
                        PSheetActionRow(UiRes.drawable.ui_drawable_smartphone, stringResource(Res.string.add_to_home), trailing = { AddToHomeHelpAction() }) {
                            showAddToHomeDialog = true
                        }
                    }
                    PSheetActionRow(UiRes.drawable.ui_drawable_square_arrow_out_up_right, stringResource(Res.string.open_with)) {
                        audioVM.openFile(m.path)
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
