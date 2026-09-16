package com.ismartcoding.plain.ui.page.feeds

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.i18n.*
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.ui.base.BottomActionButtons
import com.ismartcoding.plain.ui.base.IconTextSmallButtonDelete
import com.ismartcoding.plain.ui.base.PIconTextSmallButton
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
            val selectedIds = feedEntriesVM.selectedIds.toSet()
            val anyUnread = feedEntriesVM.itemsFlow.collectAsState().value.any { it.id in selectedIds && !it.read }
            // Short Gmail-style labels: the action bar scrolls horizontally and
            // 4-5 char labels overflow it.
            PIconTextSmallButton(
                icon = if (anyUnread) Res.drawable.circle_check else Res.drawable.circle_dot,
                text = stringResource(if (anyUnread) Res.string.read else Res.string.unread),
                click = {
                    feedEntriesVM.markRead(selectedIds, anyUnread)
                    feedEntriesVM.exitSelectMode()
                },
            )
            IconTextSmallButtonDelete {
                feedEntriesVM.delete(tagsVM, feedEntriesVM.selectedIds.toSet())
                feedEntriesVM.exitSelectMode()
            }
        }
    }
}