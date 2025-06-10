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
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.helpers.ShareHelper
import com.ismartcoding.plain.ui.base.BottomActionButtons
import com.ismartcoding.plain.ui.base.IconTextSmallButtonDelete
import com.ismartcoding.plain.ui.base.IconTextSmallButtonLabel
import com.ismartcoding.plain.ui.base.IconTextSmallButtonLabelOff
import com.ismartcoding.plain.ui.base.IconTextSmallButtonRestore
import com.ismartcoding.plain.ui.base.IconTextSmallButtonShare
import com.ismartcoding.plain.ui.base.IconTextSmallButtonTrash
import com.ismartcoding.plain.ui.base.PBottomAppBar
import com.ismartcoding.plain.ui.base.dragselect.DragSelectState
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.AudioViewModel
import com.ismartcoding.plain.ui.models.BaseMediaViewModel
import com.ismartcoding.plain.ui.models.ImagesViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.models.VideosViewModel
import com.ismartcoding.plain.ui.page.tags.BatchSelectTagsDialog

@SuppressLint("ResourceType")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun <T : IData> MediaFilesSelectModeBottomActions(
    vm: BaseMediaViewModel<T>,
    tagsVM: TagsViewModel,
    tagsState: List<DTag>,
    dragSelectState: DragSelectState,
    getItemUri: (String) -> android.net.Uri,
    getCollectableItems: @Composable () -> List<T>,
    isInTrashMode: Boolean
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
            if (AppFeatureType.MEDIA_TRASH.has()) {
                if (isInTrashMode) {
                    IconTextSmallButtonRestore {
                        @Suppress("UNCHECKED_CAST")
                        when (vm) {
                            is ImagesViewModel -> vm.restore(context, tagsVM, dragSelectState.selectedIds.toSet())
                            is VideosViewModel -> vm.restore(context, tagsVM, dragSelectState.selectedIds.toSet())
                            is AudioViewModel -> vm.restore(context, tagsVM, dragSelectState.selectedIds.toSet())
                        }
                        dragSelectState.exitSelectMode()
                    }
                    IconTextSmallButtonDelete {
                        DialogHelper.confirmToDelete {
                            @Suppress("UNCHECKED_CAST")
                            when (vm) {
                                is ImagesViewModel -> vm.delete(context, tagsVM, dragSelectState.selectedIds.toSet())
                                is VideosViewModel -> vm.delete(context, tagsVM, dragSelectState.selectedIds.toSet())
                                is AudioViewModel -> vm.delete(context, tagsVM, dragSelectState.selectedIds.toSet())
                            }
                            dragSelectState.exitSelectMode()
                        }
                    }
                } else {
                    IconTextSmallButtonTrash {
                        @Suppress("UNCHECKED_CAST")
                        when (vm) {
                            is ImagesViewModel -> vm.trash(context, tagsVM, dragSelectState.selectedIds.toSet())
                            is VideosViewModel -> vm.trash(context, tagsVM, dragSelectState.selectedIds.toSet())
                            is AudioViewModel -> vm.trash(context, tagsVM, dragSelectState.selectedIds.toSet())
                        }
                        dragSelectState.exitSelectMode()
                    }
                }
            } else {
                IconTextSmallButtonDelete {
                    DialogHelper.confirmToDelete {
                        @Suppress("UNCHECKED_CAST")
                        when (vm) {
                            is ImagesViewModel -> vm.delete(context, tagsVM, dragSelectState.selectedIds.toSet())
                            is VideosViewModel -> vm.delete(context, tagsVM, dragSelectState.selectedIds.toSet())
                            is AudioViewModel -> vm.delete(context, tagsVM, dragSelectState.selectedIds.toSet())
                        }
                        dragSelectState.exitSelectMode()
                    }
                }
            }
        }
    }
} 