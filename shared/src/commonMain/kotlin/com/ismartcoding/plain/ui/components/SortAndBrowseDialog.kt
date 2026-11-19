package com.ismartcoding.plain.ui.components

import androidx.compose.runtime.Composable
import com.ismartcoding.plain.IData
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.models.MediaSortBy
import com.ismartcoding.plain.ui.files.getTextId
import com.ismartcoding.plain.ui.media.BaseMediaViewModel
import org.jetbrains.compose.resources.stringResource

@Composable
fun <T : IData> SortAndBrowseDialog(
    mediaVM: BaseMediaViewModel<T>,
    sortByEntries: List<MediaSortBy>,
    onSortSelected: (MediaSortBy) -> Unit = {},
    onDismiss: () -> Unit = {},
) {
    RadioDialog(
        title = stringResource(Res.string.sort),
        options = sortByEntries.map { sortBy ->
            RadioDialogOption(
                text = stringResource(sortBy.getTextId()),
                selected = mediaVM.sortBy.value == sortBy,
                onClick = {
                    mediaVM.sortBy.value = sortBy
                    onSortSelected(sortBy)
                },
            )
        },
        dismissOnSelect = false,
        onDismissRequest = onDismiss,
    )
}
