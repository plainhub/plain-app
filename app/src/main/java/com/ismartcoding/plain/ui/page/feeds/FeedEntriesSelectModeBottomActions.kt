package com.ismartcoding.plain.ui.page.feeds

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.ui.base.BottomActionButtons
import com.ismartcoding.plain.ui.base.IconTextSmallButtonDelete
import com.ismartcoding.plain.ui.base.IconTextSmallButtonLabel
import com.ismartcoding.plain.ui.base.IconTextSmallButtonLabelOff
import com.ismartcoding.plain.ui.base.PBottomAppBar
import com.ismartcoding.plain.ui.models.FeedEntriesViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.models.exitSelectMode
import com.ismartcoding.plain.ui.models.getSelectedItems
import com.ismartcoding.plain.ui.page.tags.BatchSelectTagsDialog

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FeedEntriesSelectModeBottomActions(
    feedEntriesVM: FeedEntriesViewModel,
    tagsVM: TagsViewModel,
    tagsState: List<DTag>,
) {
    var showSelectTagsDialog by remember {
        mutableStateOf(false)
    }
    var removeFromTags by remember {
        mutableStateOf(false)
    }

    if (showSelectTagsDialog) {
        BatchSelectTagsDialog(tagsVM, tagsState, feedEntriesVM.getSelectedItems(), removeFromTags) {
            showSelectTagsDialog = false
            feedEntriesVM.exitSelectMode()
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
            IconTextSmallButtonDelete {
                feedEntriesVM.delete(tagsVM, feedEntriesVM.selectedIds.toSet())
                feedEntriesVM.exitSelectMode()
            }
        }
    }
}