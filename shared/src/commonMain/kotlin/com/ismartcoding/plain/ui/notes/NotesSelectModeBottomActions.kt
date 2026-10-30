package com.ismartcoding.plain.ui.notes

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.ismartcoding.plain.models.Tag
import com.ismartcoding.plain.ui.base.BottomActionButtons
import com.ismartcoding.plain.ui.components.IconTextSmallButtonDelete
import com.ismartcoding.plain.ui.components.IconTextSmallButtonLabel
import com.ismartcoding.plain.ui.components.IconTextSmallButtonLabelOff
import com.ismartcoding.plain.ui.components.IconTextSmallButtonRestore
import com.ismartcoding.plain.ui.components.IconTrashButton
import com.ismartcoding.plain.ui.base.PBottomAppBar
import com.ismartcoding.plain.ui.notes.NotesViewModel
import com.ismartcoding.plain.ui.tags.TagsViewModel
import com.ismartcoding.plain.ui.state.exitSelectMode
import com.ismartcoding.plain.ui.state.getSelectedItems
import com.ismartcoding.plain.ui.tags.BatchSelectTagsDialog

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NotesSelectModeBottomActions(
    tagsVM: TagsViewModel,
    tagsState: List<Tag>,
    notesVM: NotesViewModel = NotesViewModel.local,
) {
    var showSelectTagsDialog by remember {
        mutableStateOf(false)
    }
    var removeFromTags by remember {
        mutableStateOf(false)
    }

    if (showSelectTagsDialog) {
        BatchSelectTagsDialog(tagsVM, tagsState, notesVM.getSelectedItems(), removeFromTags) {
            showSelectTagsDialog = false
            notesVM.exitSelectMode()
        }
    }

    PBottomAppBar {
        BottomActionButtons {
            if (notesVM.trash.value) {
                IconTextSmallButtonRestore {
                    notesVM.restore(tagsVM, notesVM.selectedIds.toSet())
                    notesVM.exitSelectMode()
                }
                IconTextSmallButtonDelete {
                    notesVM.delete(tagsVM, notesVM.selectedIds.toSet())
                    notesVM.exitSelectMode()
                }
            } else {
                IconTextSmallButtonLabel {
                    showSelectTagsDialog = true
                    removeFromTags = false
                }
                IconTextSmallButtonLabelOff {
                    showSelectTagsDialog = true
                    removeFromTags = true
                }
                IconTrashButton {
                    notesVM.trash(tagsVM, notesVM.selectedIds.toSet())
                    notesVM.exitSelectMode()
                }
            }
        }
    }
}
