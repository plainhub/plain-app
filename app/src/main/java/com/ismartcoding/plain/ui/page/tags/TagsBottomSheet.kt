package com.ismartcoding.plain.ui.page.tags

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.base.ActionButtonAdd
import com.ismartcoding.plain.ui.base.ActionButtonRefresh
import com.ismartcoding.plain.ui.base.NoDataColumn
import com.ismartcoding.plain.ui.base.PBottomSheetTopAppBar
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.components.TagNameDialog
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.theme.PlainTheme
import com.ismartcoding.plain.ui.theme.red
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TagsBottomSheet(
    tagsVM: TagsViewModel,
    onDismissRequest: () -> Unit
) {
    val itemsState by tagsVM.itemsFlow.collectAsState()
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    val scope = rememberCoroutineScope()

    PModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
    ) {
        Column {
            PBottomSheetTopAppBar(
                titleContent = {
                    Text(
                        text = stringResource(R.string.tags),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                actions = {
                    ActionButtonRefresh(
                        loading = tagsVM.showLoading.value,
                        onClick = {
                            tagsVM.showLoading.value = true
                            scope.launch {
                                withIO { tagsVM.loadAsync() }
                            }
                        }
                    )
                    ActionButtonAdd { tagsVM.showAddDialog() }
                }
            )

            if (itemsState.isNotEmpty()) {
                LazyColumn(
                    Modifier.fillMaxSize()
                ) {
                    item {
                        TopSpace()
                    }
                    items(itemsState) { tag ->
                        PListItem(
                            modifier = PlainTheme
                                .getCardModifier()
                                .clickable {
                                    tagsVM.showEditDialog(tag)
                                },
                            title = tag.name,
                            action = {
                                PIconButton(
                                    icon = R.drawable.delete_forever,
                                    tint = MaterialTheme.colorScheme.red,
                                    contentDescription = stringResource(R.string.delete),
                                    click = {
                                        DialogHelper.confirmToDelete {
                                            tagsVM.deleteTag(tag.id)
                                        }
                                    }
                                )
                            }
                        )
                        VerticalSpace(dp = 8.dp)
                    }
                }
            } else {
                NoDataColumn(loading = tagsVM.showLoading.value)
            }
        }
    }

    TagNameDialog(tagsVM)
} 