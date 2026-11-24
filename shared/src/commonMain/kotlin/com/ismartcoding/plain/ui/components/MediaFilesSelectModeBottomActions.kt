package com.ismartcoding.plain.ui.components

import com.ismartcoding.plain.ui.media.MediaSelectionTrashActions
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.ismartcoding.plain.IData
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.ui.state.launchSafe
import com.ismartcoding.plain.models.Tag
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.platform.system.shareFiles
import com.ismartcoding.plain.ui.base.BottomActionButtons
import com.ismartcoding.plain.ui.base.PBottomAppBar
import com.ismartcoding.plain.ui.base.dragselect.DragSelectState
import com.ismartcoding.plain.ui.media.BaseMediaViewModel
import com.ismartcoding.plain.ui.tags.TagsViewModel
import com.ismartcoding.plain.ui.tags.BatchSelectTagsDialog
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun <T : IData> MediaFilesSelectModeBottomActions(
    vm: BaseMediaViewModel<T>,
    tagsVM: TagsViewModel,
    tagsState: List<Tag>,
    dragSelectState: DragSelectState,
    getCollectableItems: @Composable () -> List<T>,
    isInTrashMode: Boolean
) {
    val scope = rememberCoroutineScope()
    var showSelectTagsDialog by remember {
        mutableStateOf(false)
    }
    var removeFromTags by remember {
        mutableStateOf(false)
    }

    if (showSelectTagsDialog) {
        val selectedItems = getCollectableItems().filter { it.id in dragSelectState.selectedIds }
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
            IconTextSmallButtonShare {
                val ids = dragSelectState.selectedIds.toList()
                scope.launchSafe {
                    val uris = withIO { vm.source.catalog.shareUris(vm.dataType, ids) }
                    shareFiles(uris)
                }
            }
            MediaSelectionTrashActions(vm, tagsVM, dragSelectState, isInTrashMode)
        }
    }
}
