package com.ismartcoding.plain.ui.components

import com.ismartcoding.plain.i18n.*

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.db.IData
import com.ismartcoding.plain.enums.ButtonSize
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.ui.base.PDialogRadioRow
import com.ismartcoding.plain.ui.base.PTextButton
import com.ismartcoding.plain.ui.models.BaseMediaViewModel
import com.ismartcoding.plain.ui.theme.dialogSheetBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T : IData> SortAndBrowseDialog(
    mediaVM: BaseMediaViewModel<T>,
    sortByEntries: List<FileSortBy>,
    onSortSelected: (FileSortBy) -> Unit = {},
    onDismiss: () -> Unit = {},
) {
    AlertDialog(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.dialogSheetBackground,
        onDismissRequest = onDismiss,
        title = null,
        text = {
            LazyColumn {
                item {
                    Text(
                        text = stringResource(Res.string.sort),
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
                        text = stringResource(sortByOption.getTextId())
                    )
                }
            }
        },
        confirmButton = {
            PTextButton(text = stringResource(Res.string.close), buttonSize = ButtonSize.MEDIUM, onClick = onDismiss)
        },
        dismissButton = {},
    )
}
