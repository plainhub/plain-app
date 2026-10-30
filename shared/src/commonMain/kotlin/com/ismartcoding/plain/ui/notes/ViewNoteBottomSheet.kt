package com.ismartcoding.plain.ui.notes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.models.Tag
import com.ismartcoding.plain.models.TagRelation
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.platform.system.formatDateTime
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import com.ismartcoding.plain.ui.base.PSheetPrimaryAction
import com.ismartcoding.plain.ui.base.PSheetPrimaryActionsCard
import com.ismartcoding.plain.ui.base.PSheetPrimaryDeleteAction
import com.ismartcoding.plain.ui.base.PSheetPrimaryTrashAction
import com.ismartcoding.plain.ui.base.PSectionTitle
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.components.TagSelector
import com.ismartcoding.plain.ui.notes.NotesViewModel
import com.ismartcoding.plain.ui.tags.TagsViewModel
import com.ismartcoding.plain.ui.state.enterSelectMode
import com.ismartcoding.plain.ui.state.select
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.archive_restore as ui_drawable_archive_restore
import com.ismartcoding.plain.ui.resources.list_checks as ui_drawable_list_checks
import com.ismartcoding.plain.ui.theme.PlainTheme
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ViewNoteBottomSheet(
    tagsVM: TagsViewModel,
    tagsMap: Map<String, List<TagRelation>>,
    tagsState: List<Tag>,
    notesVM: NotesViewModel = NotesViewModel.local,
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
                VerticalSpace(16.dp)
            }
            item {
                PSheetPrimaryActionsCard {
                    if (!notesVM.showSearchBar.value) {
                        PSheetPrimaryAction(UiRes.drawable.ui_drawable_list_checks, stringResource(Res.string.select)) {
                            notesVM.enterSelectMode()
                            notesVM.select(m.id)
                            onDismiss()
                        }
                    }
                    if (notesVM.trash.value) {
                        PSheetPrimaryAction(UiRes.drawable.ui_drawable_archive_restore, stringResource(Res.string.restore)) {
                            notesVM.restore(tagsVM, setOf(m.id))
                            onDismiss()
                        }
                        PSheetPrimaryDeleteAction(stringResource(Res.string.delete)) {
                            notesVM.delete(tagsVM, setOf(m.id))
                            onDismiss()
                        }
                    } else {
                        PSheetPrimaryTrashAction(stringResource(Res.string.trash)) {
                            notesVM.trash(tagsVM, setOf(m.id))
                            onDismiss()
                        }
                    }
                }
            }
            if (!notesVM.trash.value) {
                item {
                    VerticalSpace(dp = 16.dp)
                    PSectionTitle(title = stringResource(Res.string.tags))
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
                PCard(modifier = Modifier.padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN)) {
                    PListItem(title = stringResource(Res.string.created_at), value = m.createdAt.formatDateTime())
                    PListItem(title = stringResource(Res.string.updated_at), value = m.updatedAt.formatDateTime())
                }
            }
            item {
                BottomSpace()
            }
        }
    }
}
