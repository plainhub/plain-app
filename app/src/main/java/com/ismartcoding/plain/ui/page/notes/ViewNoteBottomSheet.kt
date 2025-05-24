package com.ismartcoding.plain.ui.page.notes

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.db.DTagRelation
import com.ismartcoding.plain.extensions.formatDateTime
import com.ismartcoding.plain.ui.base.ActionButtons
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.IconTextTrashButton
import com.ismartcoding.plain.ui.base.IconTextDeleteButton
import com.ismartcoding.plain.ui.base.IconTextRestoreButton
import com.ismartcoding.plain.ui.base.IconTextSelectButton
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import com.ismartcoding.plain.ui.base.Subtitle
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.components.TagSelector
import com.ismartcoding.plain.ui.models.NotesViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.models.enterSelectMode
import com.ismartcoding.plain.ui.models.select
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ViewNoteBottomSheet(
    notesVM: NotesViewModel,
    tagsVM: TagsViewModel,
    tagsMap: Map<String, List<DTagRelation>>,
    tagsState: List<DTag>,
) {
    val m = notesVM.selectedItem.value ?: return
    val onDismiss = {
        notesVM.selectedItem.value = null
    }
    val scope = rememberCoroutineScope()

    PModalBottomSheet(
        onDismissRequest = {
            onDismiss()
        },
    ) {
        LazyColumn {
            item {
                VerticalSpace(32.dp)
            }
            item {
                ActionButtons {
                    if (!notesVM.showSearchBar.value) {
                        IconTextSelectButton {
                            notesVM.enterSelectMode()
                            notesVM.select(m.id)
                            onDismiss()
                        }
                    }
                    if (notesVM.trash.value) {
                        IconTextRestoreButton {
                            notesVM.restore(tagsVM, setOf(m.id))
                            onDismiss()
                        }
                        IconTextDeleteButton {
                            notesVM.delete(tagsVM, setOf(m.id))
                            onDismiss()
                        }
                    } else {
                        IconTextTrashButton {
                            notesVM.trash(tagsVM, setOf(m.id))
                            onDismiss()
                        }
                    }
                }
            }
            if (!notesVM.trash.value) {
                item {
                    VerticalSpace(dp = 16.dp)
                    Subtitle(text = stringResource(id = R.string.tags))
                    TagSelector(
                        data = m,
                        tagsVM = tagsVM,
                        tagsMap = tagsMap,
                        tagsState = tagsState,
                        onChangedAsync = {
                            notesVM.loadAsync(tagsVM)
                        }
                    )
                }
            }
            item {
                VerticalSpace(dp = 16.dp)
                PCard {
                    PListItem(title = stringResource(id = R.string.created_at), value = m.createdAt.formatDateTime())
                    PListItem(title = stringResource(id = R.string.updated_at), value = m.updatedAt.formatDateTime())
                }
            }
            item {
                BottomSpace()
            }
        }
    }
}


