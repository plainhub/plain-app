package com.ismartcoding.plain.ui.page.audio.components

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.features.media.AudioMediaStoreHelper
import com.ismartcoding.plain.helpers.ShareHelper
import com.ismartcoding.plain.ui.base.ActionButtons
import com.ismartcoding.plain.ui.base.BottomActionButtons
import com.ismartcoding.plain.ui.base.IconTextSmallButtonDelete
import com.ismartcoding.plain.ui.base.IconTextSmallButtonLabel
import com.ismartcoding.plain.ui.base.IconTextSmallButtonLabelOff
import com.ismartcoding.plain.ui.base.IconTextSmallButtonPlaylistAdd
import com.ismartcoding.plain.ui.base.IconTextSmallButtonShare
import com.ismartcoding.plain.ui.base.PBottomAppBar
import com.ismartcoding.plain.ui.base.dragselect.DragSelectState
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModel
import com.ismartcoding.plain.ui.models.AudioViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.page.tags.BatchSelectTagsDialog
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AudioFilesSelectModeBottomActions(
    audioVM: AudioViewModel,
    audioPlaylistVM: AudioPlaylistViewModel,
    tagsVM: TagsViewModel,
    tagsState: List<DTag>,
    dragSelectState: DragSelectState,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showSelectTagsDialog by remember { mutableStateOf(false) }
    var removeFromTags by remember { mutableStateOf(false) }

    if (showSelectTagsDialog) {
        val selectedIds = dragSelectState.selectedIds
        val selectedItems = audioVM.itemsFlow.value.filter { selectedIds.contains(it.id) }
        BatchSelectTagsDialog(tagsVM, tagsState, selectedItems, removeFromTags) {
            showSelectTagsDialog = false
            dragSelectState.exitSelectMode()
        }
    }

    PBottomAppBar {
        BottomActionButtons {
            IconTextSmallButtonLabel {
                showSelectTagsDialog = true
                removeFromTags = false
            }
            IconTextSmallButtonLabelOff {
                showSelectTagsDialog = true
                removeFromTags = true
            }
            IconTextSmallButtonPlaylistAdd {
                scope.launch {
                    val selectedIds = dragSelectState.selectedIds
                    val selectedItems = audioVM.itemsFlow.value.filter { selectedIds.contains(it.id) }
                    withIO {
                        audioPlaylistVM.addAsync(context, selectedItems)
                    }
                    dragSelectState.exitSelectMode()
                    DialogHelper.showMessage(R.string.added_to_playlist)
                }
            }
            IconTextSmallButtonShare {
                ShareHelper.shareUris(context, dragSelectState.selectedIds.map { AudioMediaStoreHelper.getItemUri(it) })
            }
            IconTextSmallButtonDelete {
                DialogHelper.confirmToDelete {
                    audioVM.delete(context, tagsVM, dragSelectState.selectedIds.toSet())
                    dragSelectState.exitSelectMode()
                }
            }
        }
    }
} 