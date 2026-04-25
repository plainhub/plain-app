package com.ismartcoding.plain.ui.components

import android.content.Context
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.IData
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.preferences.DocTabsModePreference
import com.ismartcoding.plain.ui.base.PDialogRadioRow
import com.ismartcoding.plain.ui.models.BaseMediaViewModel
import com.ismartcoding.plain.ui.models.DocsViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T : IData> SortAndBrowseDialog(
    context: Context,
    mediaVM: BaseMediaViewModel<T>,
    tagsVM: TagsViewModel,
    sortByEntries: List<FileSortBy> = FileSortBy.entries.filter { it != FileSortBy.TAKEN_AT_DESC },
    onSortSelected: (FileSortBy) -> Unit = {},
    onDismiss: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val isDocs = mediaVM.dataType == DataType.DOC
    val docsVM = (mediaVM as? DocsViewModel)

    AlertDialog(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = onDismiss,
        title = null,
        text = {
            LazyColumn {
                // View Mode section (only for docs)
                if (isDocs && docsVM != null) {
                    item {
                        Text(
                            text = stringResource(R.string.view_mode),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    // Browse by tags option
                    item {
                        PDialogRadioRow(
                            selected = docsVM.tabsShowTags.value,
                            onClick = {
                                scope.launch(Dispatchers.IO) {
                                    DocTabsModePreference.putAsync(context, true)
                                    docsVM.tabsShowTags.value = true
                                    docsVM.fileType.value = ""
                                    docsVM.tag.value = null
                                    docsVM.trash.value = false
                                    docsVM.loadAsync(context, tagsVM)
                                }
                            },
                            text = stringResource(R.string.browse_by_tags)
                        )
                    }
                    // Browse by file type option
                    item {
                        PDialogRadioRow(
                            selected = !docsVM.tabsShowTags.value,
                            onClick = {
                                scope.launch(Dispatchers.IO) {
                                    DocTabsModePreference.putAsync(context, false)
                                    docsVM.tabsShowTags.value = false
                                    docsVM.fileType.value = ""
                                    docsVM.tag.value = null
                                    docsVM.trash.value = false
                                    docsVM.loadAsync(context, tagsVM)
                                }
                            },
                            text = stringResource(R.string.browse_by_file_type)
                        )
                    }
                    // Spacing between sections
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // Sort rules section
                item {
                    Text(
                        text = stringResource(R.string.sort),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Sort options
                items(sortByEntries) { sortByOption ->
                    PDialogRadioRow(
                        selected = mediaVM.sortBy.value == sortByOption,
                        onClick = {
                            mediaVM.sortBy.value = sortByOption
                            onSortSelected(sortByOption)
                        },
                        text = stringResource(id = sortByOption.getTextId())
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.close))
            }
        },
        dismissButton = {},
    )
}
