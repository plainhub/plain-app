package com.ismartcoding.plain.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.res.stringResource
import com.ismartcoding.plain.R
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.ui.base.RadioDialog
import com.ismartcoding.plain.ui.base.RadioDialogOption

@Composable
fun FileSortDialog(sortBy: MutableState<FileSortBy>, onSelected: (FileSortBy) -> Unit, onDismiss: () -> Unit = {}) {
    RadioDialog(
        title = stringResource(R.string.sort),
        options =
        FileSortBy.entries.map {
            RadioDialogOption(
                text = stringResource(id = it.getTextId()),
                selected = it == sortBy.value,
            ) {
                onSelected(it)
            }
        },
        onDismissRequest = onDismiss
    )
}