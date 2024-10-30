package com.ismartcoding.plain.ui.components

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.ismartcoding.plain.data.IData
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.helpers.ShareHelper
import com.ismartcoding.plain.ui.base.BottomActionButtons
import com.ismartcoding.plain.ui.base.IconTextSmallButtonDelete
import com.ismartcoding.plain.ui.base.IconTextSmallButtonLabel
import com.ismartcoding.plain.ui.base.IconTextSmallButtonLabelOff
import com.ismartcoding.plain.ui.base.IconTextSmallButtonShare
import com.ismartcoding.plain.ui.base.PBottomAppBar
import com.ismartcoding.plain.ui.base.dragselect.DragSelectState
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.AudioViewModel
import com.ismartcoding.plain.ui.models.ImagesViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.models.VideosViewModel
import com.ismartcoding.plain.ui.page.tags.BatchSelectTagsDialog

@SuppressLint("ResourceType")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun <T : IData> MediaFilesSelectModeBottomActions(
    viewModel: Any,
    tagsVM: TagsViewModel,
    tagsState: List<DTag>,
    dragSelectState: DragSelectState,
    getItemUri: (String) -> android.net.Uri,
    getCollectableItems: @Composable () -> List<T>
) {
    val context = LocalContext.current
    var showSelectTagsDialog by remember {
        mutableStateOf(false)
    }
    var removeFromTags by remember {
        mutableStateOf(false)
    }

    if (showSelectTagsDialog) {
        val selectedIds = dragSelectState.selectedIds
        val selectedItems = getCollectableItems().filter { selectedIds.contains(it.id) }
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
                ShareHelper.shareUris(context, dragSelectState.selectedIds.map { getItemUri(it) })
            }
            IconTextSmallButtonDelete {
                DialogHelper.confirmToDelete {
                    @Suppress("UNCHECKED_CAST")
                    when (viewModel) {
                        is ImagesViewModel -> viewModel.delete(context, tagsVM, dragSelectState.selectedIds.toSet())
                        is VideosViewModel -> viewModel.delete(context, tagsVM, dragSelectState.selectedIds.toSet())
                        is AudioViewModel -> viewModel.delete(context, tagsVM, dragSelectState.selectedIds.toSet())
                    }
                    dragSelectState.exitSelectMode()
                }
            }
        }
    }
} 