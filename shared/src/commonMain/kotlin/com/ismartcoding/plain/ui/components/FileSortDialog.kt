package com.ismartcoding.plain.ui.components

import com.ismartcoding.plain.i18n.*

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.ui.base.RadioDialog
import com.ismartcoding.plain.ui.base.RadioDialogOption

@Composable
fun FileSortDialog(sortBy: MutableState<FileSortBy>, entries: List<FileSortBy> = FileSortBy.entries, onSelected: (FileSortBy) -> Unit, onDismiss: () -> Unit = {}) {
    RadioDialog(
        title = stringResource(Res.string.sort),
        options =
        entries.map {
            RadioDialogOption(
                text = stringResource(it.getTextId()),
                selected = it == sortBy.value,
            ) {
                onSelected(it)
            }
        },
        onDismissRequest = onDismiss
    )
}