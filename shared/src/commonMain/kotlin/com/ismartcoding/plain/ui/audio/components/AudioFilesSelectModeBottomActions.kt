package com.ismartcoding.plain.ui.audio.components

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.ismartcoding.plain.models.Audio
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.ismartcoding.plain.platform.system.AppFeatureType
import com.ismartcoding.plain.platform.system.DataType
import com.ismartcoding.plain.platform.system.has
import com.ismartcoding.plain.models.Tag
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.ui.state.launchSafe
import com.ismartcoding.plain.platform.system.shareFiles
import com.ismartcoding.plain.ui.base.BottomActionButtons
import com.ismartcoding.plain.ui.components.IconTextSmallButtonDelete
import com.ismartcoding.plain.ui.components.IconTextSmallButtonLabel
import com.ismartcoding.plain.ui.components.IconTextSmallButtonLabelOff
import com.ismartcoding.plain.ui.audio.components.IconTextSmallButtonQueueAdd
import com.ismartcoding.plain.ui.components.IconTextSmallButtonRestore
import com.ismartcoding.plain.ui.components.IconTextSmallButtonShare
import com.ismartcoding.plain.ui.components.IconTextSmallButtonTrash
import com.ismartcoding.plain.ui.base.PBottomAppBar
import com.ismartcoding.plain.ui.base.PIconTextSmallButton
import com.ismartcoding.plain.ui.base.dragselect.DragSelectState
import com.ismartcoding.plain.ui.components.DialogHelper
import com.ismartcoding.plain.ui.components.confirmActionAsync
import com.ismartcoding.plain.ui.audio.AudioQueueViewModel
import com.ismartcoding.plain.ui.audio.AudioViewModel
import com.ismartcoding.plain.ui.tags.TagsViewModel
import com.ismartcoding.plain.ui.tags.BatchSelectTagsDialog
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.playlist_remove as ui_drawable_playlist_remove
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AudioFilesSelectModeBottomActions(
    audioVM: AudioViewModel,
    audioQueueVM: AudioQueueViewModel,
    tagsVM: TagsViewModel,
    tagsState: List<Tag>,
    dragSelectState: DragSelectState,
    items: List<Audio>,
    // Generic slot (playlist detail, artist page, ...): when provided, a
    // remove-from-playlist action leads the bar and the slot runs the removal.
    removeFromPlaylist: (() -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()
    var showSelectTagsDialog by remember { mutableStateOf(false) }
    var removeFromTags by remember { mutableStateOf(false) }

    val selectedItems = items.filter { it.id in dragSelectState.selectedIds }

    if (showSelectTagsDialog) {
        BatchSelectTagsDialog(tagsVM, tagsState, selectedItems, removeFromTags) {
            showSelectTagsDialog = false
            dragSelectState.exitSelectMode()
        }
    }

    PBottomAppBar {
        BottomActionButtons {
            if (removeFromPlaylist != null) {
                PIconTextSmallButton(UiRes.drawable.ui_drawable_playlist_remove, stringResource(Res.string.remove_from_playlist), onClick = removeFromPlaylist)
            }
            if (!audioVM.trash.value) {
                IconTextSmallButtonLabel {
                    showSelectTagsDialog = true
                    removeFromTags = false
                }
                IconTextSmallButtonLabelOff {
                    showSelectTagsDialog = true
                    removeFromTags = true
                }
                IconTextSmallButtonQueueAdd {
                    scope.launch {
                        withIO {
                            audioQueueVM.addAsync(selectedItems)
                        }
                        dragSelectState.exitSelectMode()
                        DialogHelper.showMessage(Res.string.added_to_queue)
                    }
                }
                IconTextSmallButtonShare {
                    val ids = dragSelectState.selectedIds.toList()
                    scope.launchSafe {
                        val uris = withIO { audioVM.source.catalog.shareUris(DataType.AUDIO, ids) }
                        shareFiles(uris)
                    }
                }
            }
            if (AppFeatureType.MEDIA_TRASH.has(audioVM.source.capabilities)) {
                if (audioVM.trash.value) {
                    IconTextSmallButtonRestore {
                        audioVM.restore(tagsVM, dragSelectState.selectedIds.toSet())
                        dragSelectState.exitSelectMode()
                    }
                    IconTextSmallButtonDelete {
                        scope.launch {
                            confirmActionAsync(
                                Res.string.delete,
                                Res.string.confirm_to_delete,
                                callback = {
                                    audioVM.delete(tagsVM, dragSelectState.selectedIds.toSet())
                                    dragSelectState.exitSelectMode()
                                },
                                danger = true
                            )
                        }
                    }
                } else {
                    IconTextSmallButtonTrash {
                        audioVM.trash(tagsVM, dragSelectState.selectedIds.toSet())
                        dragSelectState.exitSelectMode()
                    }
                }
            } else {
                IconTextSmallButtonDelete {
                    scope.launch {
                        confirmActionAsync(
                            Res.string.delete,
                            Res.string.confirm_to_delete,
                            callback = {
                                audioVM.delete(tagsVM, dragSelectState.selectedIds.toSet())
                                dragSelectState.exitSelectMode()
                            },
                            danger = true
                        )
                    }
                }
            }
        }
    }
}
