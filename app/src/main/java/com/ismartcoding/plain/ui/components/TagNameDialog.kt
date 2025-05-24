package com.ismartcoding.plain.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.base.TextFieldDialog
import com.ismartcoding.plain.ui.models.TagsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun TagNameDialog(tagsVM: TagsViewModel, onChangedAsync: suspend () -> Unit = {}) {
    val tag = tagsVM.editItem.value
    val scope = rememberCoroutineScope()
    if (tagsVM.tagNameDialogVisible.value) {
        TextFieldDialog(
            title = stringResource(id = if (tag != null) R.string.edit_tag else R.string.add_tag),
            value = tagsVM.editTagName.value,
            placeholder = tag?.name ?: stringResource(id = R.string.name),
            onValueChange = {
                tagsVM.editTagName.value = it
            },
            onDismissRequest = {
                tagsVM.tagNameDialogVisible.value = false
            },
            confirmText = stringResource(id = R.string.save),
            onConfirm = {
                scope.launch(Dispatchers.IO) {
                    if (tag != null) {
                        tagsVM.editTagAsync(tagsVM.editTagName.value)
                    } else {
                        tagsVM.addTagAsync(tagsVM.editTagName.value)
                    }
                    onChangedAsync()
                }

            },
        )
    }
}