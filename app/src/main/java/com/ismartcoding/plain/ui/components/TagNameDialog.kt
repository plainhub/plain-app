package com.ismartcoding.plain.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.base.TextFieldDialog
import com.ismartcoding.plain.ui.models.TagsViewModel

@Composable
fun TagNameDialog(viewModel: TagsViewModel, onChanged: () -> Unit = {}) {
    val tag = viewModel.editItem.value
    if (viewModel.tagNameDialogVisible.value) {
        TextFieldDialog(
            title = stringResource(id = if (tag != null) R.string.edit_tag else R.string.add_tag),
            value = viewModel.editTagName.value,
            placeholder = tag?.name ?: stringResource(id = R.string.name),
            onValueChange = {
                viewModel.editTagName.value = it
            },
            onDismissRequest = {
                viewModel.tagNameDialogVisible.value = false
            },
            confirmText = stringResource(id = R.string.save),
            onConfirm = {
                if (tag != null) {
                    viewModel.editTag(viewModel.editTagName.value)
                } else {
                    viewModel.addTag(viewModel.editTagName.value)
                }
                onChanged()
            },
        )
    }
}