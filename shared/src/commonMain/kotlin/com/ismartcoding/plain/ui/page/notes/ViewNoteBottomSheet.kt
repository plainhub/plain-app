package com.ismartcoding.plain.ui.page.notes
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import com.ismartcoding.plain.ui.theme.PlainTheme

import com.ismartcoding.plain.i18n.*

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.db.DTagRelation
import com.ismartcoding.plain.platform.formatDateTime
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import com.ismartcoding.plain.ui.base.PSheetPrimaryAction
import com.ismartcoding.plain.ui.base.PSheetPrimaryActionsCard
import com.ismartcoding.plain.ui.base.Subtitle
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.components.TagSelector
import com.ismartcoding.plain.ui.models.NotesViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.models.enterSelectMode
import com.ismartcoding.plain.ui.models.select
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ViewNoteBottomSheet(
    tagsVM: TagsViewModel,
    tagsMap: Map<String, List<DTagRelation>>,
    tagsState: List<DTag>,
) {
    val m = NotesViewModel.selectedItem.value ?: return
    val onDismiss = {
        NotesViewModel.selectedItem.value = null
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
                    if (!NotesViewModel.showSearchBar.value) {
                        PSheetPrimaryAction(Res.drawable.list_checks, stringResource(Res.string.select)) {
                            NotesViewModel.enterSelectMode()
                            NotesViewModel.select(m.id)
                            onDismiss()
                        }
                    }
                    if (NotesViewModel.trash.value) {
                        PSheetPrimaryAction(Res.drawable.archive_restore, stringResource(Res.string.restore)) {
                            NotesViewModel.restore(tagsVM, setOf(m.id))
                            onDismiss()
                        }
                        PSheetPrimaryAction(
                            Res.drawable.delete_forever,
                            stringResource(Res.string.delete),
                            container = MaterialTheme.colorScheme.errorContainer,
                            tint = MaterialTheme.colorScheme.error,
                        ) {
                            NotesViewModel.delete(tagsVM, setOf(m.id))
                            onDismiss()
                        }
                    } else {
                        PSheetPrimaryAction(
                            Res.drawable.trash_2,
                            stringResource(Res.string.trash),
                            container = MaterialTheme.colorScheme.errorContainer,
                            tint = MaterialTheme.colorScheme.error,
                        ) {
                            NotesViewModel.trash(tagsVM, setOf(m.id))
                            onDismiss()
                        }
                    }
                }
            }
            if (!NotesViewModel.trash.value) {
                item {
                    VerticalSpace(dp = 16.dp)
                    Subtitle(text = stringResource(Res.string.tags))
                    TagSelector(
                        data = m,
                        tagsVM = tagsVM,
                        tagsMap = tagsMap,
                        tagsState = tagsState,
                        onChangedAsync = {
                            NotesViewModel.loadAsync(tagsVM)
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
