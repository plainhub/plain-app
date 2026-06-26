package com.ismartcoding.plain.ui.components

import com.ismartcoding.plain.i18n.*

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.ui.base.TextFieldDialog
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.helpers.IODispatcher
import kotlinx.coroutines.launch

@Composable
fun TagNameDialog(tagsVM: TagsViewModel, onChangedAsync: suspend () -> Unit = {}) {
    val tag = tagsVM.editItem.value
    val scope = rememberCoroutineScope()
    if (tagsVM.tagNameDialogVisible.value) {
        TextFieldDialog(
            title = stringResource(if (tag != null) Res.string.edit_tag else Res.string.add_tag),
            value = tagsVM.editTagName.value,
            placeholder = tag?.name ?: stringResource(Res.string.name),
            onValueChange = {
                tagsVM.editTagName.value = it
            },
            onDismissRequest = {
                tagsVM.tagNameDialogVisible.value = false
            },
            confirmText = stringResource(Res.string.save),
            onConfirm = {
                scope.launch(IODispatcher) {
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