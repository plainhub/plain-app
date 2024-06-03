package com.ismartcoding.plain.ui.page.notes

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.RestoreFromTrash
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.db.DTagRelation
import com.ismartcoding.plain.extensions.formatDateTime
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.ui.base.ActionButtons
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PCard
import com.ismartcoding.plain.ui.base.PIconTextActionButton
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
    viewModel: NotesViewModel,
    tagsViewModel: TagsViewModel,
    tagsMap: Map<String, List<DTagRelation>>,
    tagsState: List<DTag>,
) {
    val m = viewModel.selectedItem.value ?: return
    val onDismiss = {
        viewModel.selectedItem.value = null
    }
    val scope = rememberCoroutineScope()

    PModalBottomSheet(
        onDismissRequest = {
            onDismiss()
        },
    ) {
        LazyColumn {
            item {
                ActionButtons {
                    if (!viewModel.showSearchBar.value) {
                        PIconTextActionButton(
                            icon = Icons.Outlined.Checklist,
                            text = LocaleHelper.getString(R.string.select),
                            click = {
                                viewModel.enterSelectMode()
                                viewModel.select(m.id)
                                onDismiss()
                            }
                        )
                    }
                    if (viewModel.trash.value) {
                        PIconTextActionButton(
                            icon = Icons.Outlined.RestoreFromTrash,
                            text = LocaleHelper.getString(R.string.restore),
                            click = {
                                viewModel.untrash(tagsViewModel, setOf(m.id))
                                onDismiss()
                            }
                        )
                        PIconTextActionButton(
                            icon = Icons.Outlined.DeleteForever,
                            text = LocaleHelper.getString(R.string.delete),
                            click = {
                                viewModel.delete(tagsViewModel, setOf(m.id))
                                onDismiss()
                            }
                        )
                    } else {
//            PIconTextActionButton(
//                icon = painterResource(R.drawable.ic_keep),
//                text = stringResource(id = R.string.pin),
//                click = {
//                }
//            ),
                        PIconTextActionButton(
                            icon = Icons.Outlined.DeleteOutline,
                            text = LocaleHelper.getString(R.string.move_to_trash),
                            click = {
                                viewModel.trash(tagsViewModel, setOf(m.id))
                                onDismiss()
                            }
                        )
                    }
                }
            }
            if (!viewModel.trash.value) {
                item {
                    VerticalSpace(dp = 16.dp)
                    Subtitle(text = stringResource(id = R.string.tags))
                    TagSelector(
                        data = m,
                        tagsViewModel = tagsViewModel,
                        tagsMap = tagsMap,
                        tagsState = tagsState,
                        onChanged = {
                            scope.launch(Dispatchers.IO) {
                                viewModel.refreshTabsAsync(tagsViewModel)
                            }
                        }
                    )
                }
            }
            item {
                VerticalSpace(dp = 24.dp)
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


