package com.ismartcoding.plain.ui.files.documents

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.ismartcoding.plain.platform.system.AppFeatureType
import com.ismartcoding.plain.platform.system.has
import com.ismartcoding.plain.models.Tag
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.confirm_to_delete
import com.ismartcoding.plain.i18n.delete
import com.ismartcoding.plain.platform.system.shareFiles
import com.ismartcoding.plain.ui.base.BottomActionButtons
import com.ismartcoding.plain.ui.components.IconTextSmallButtonDelete
import com.ismartcoding.plain.ui.components.IconTextSmallButtonLabel
import com.ismartcoding.plain.ui.components.IconTextSmallButtonLabelOff
import com.ismartcoding.plain.ui.components.IconTextSmallButtonRestore
import com.ismartcoding.plain.ui.components.IconTextSmallButtonShare
import com.ismartcoding.plain.ui.components.IconTextSmallButtonTrash
import com.ismartcoding.plain.ui.base.PBottomAppBar
import com.ismartcoding.plain.ui.base.dragselect.DragSelectState
import com.ismartcoding.plain.ui.components.confirmActionAsync
import com.ismartcoding.plain.ui.files.documents.DocsViewModel
import com.ismartcoding.plain.ui.tags.TagsViewModel
import com.ismartcoding.plain.ui.tags.BatchSelectTagsDialog
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DocFilesSelectModeBottomActions(
    docsVM: DocsViewModel,
    tagsVM: TagsViewModel,
    tagsState: List<Tag>,
    dragSelectState: DragSelectState,
) {
    val scope = rememberCoroutineScope()
    var showSelectTagsDialog by remember { mutableStateOf(false) }
    var removeFromTags by remember { mutableStateOf(false) }

    if (showSelectTagsDialog) {
        val selectedIds = dragSelectState.selectedIds
        val selectedItems = docsVM.itemsFlow.collectAsState().value.filter { selectedIds.contains(it.id) }
        BatchSelectTagsDialog(tagsVM, tagsState, selectedItems, removeFromTags) {
            showSelectTagsDialog = false
            dragSelectState.exitSelectMode()
        }
    }

    PBottomAppBar {
        BottomActionButtons {
            if (!docsVM.trash.value) {
                IconTextSmallButtonLabel {
                    showSelectTagsDialog = true
                    removeFromTags = false
                }
                IconTextSmallButtonLabelOff {
                    showSelectTagsDialog = true
                    removeFromTags = true
                }
                IconTextSmallButtonShare {
                    val selectedIds = dragSelectState.selectedIds
                    docsVM.share(selectedIds.toList())
                }
            }
            if (AppFeatureType.MEDIA_TRASH.has(docsVM.source.capabilities)) {
                if (docsVM.trash.value) {
                    IconTextSmallButtonRestore {
                        docsVM.restore(tagsVM, dragSelectState.selectedIds.toSet())
                        dragSelectState.exitSelectMode()
                    }
                    IconTextSmallButtonDelete {
                        scope.launch {
                            confirmActionAsync(
                                Res.string.delete,
                                Res.string.confirm_to_delete,
                                callback = {
                                    docsVM.delete(tagsVM, dragSelectState.selectedIds.toSet())
                                    dragSelectState.exitSelectMode()
                                },
                                danger = true
                            )
                        }
                    }
                } else {
                    IconTextSmallButtonTrash {
                        docsVM.trash(tagsVM, dragSelectState.selectedIds.toSet())
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
                                docsVM.delete(tagsVM, dragSelectState.selectedIds.toSet())
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
