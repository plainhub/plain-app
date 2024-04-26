package com.ismartcoding.plain.ui.page.feeds

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.models.FeedsViewModel

@Composable
fun EditFeedDialog(viewModel: FeedsViewModel) {
    if (viewModel.showEditDialog.value) {
        val focusManager = LocalFocusManager.current
        AlertDialog(
            onDismissRequest = {
                viewModel.showEditDialog.value = false
            },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = stringResource(id = R.string.edit),
                )
            },
            title = {
                Text(text = stringResource(id = R.string.edit), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = viewModel.editName.value,
                        onValueChange = {
                            viewModel.editName.value = it
                        },
                        singleLine = true,
                        label = {
                            Text(text = stringResource(id = R.string.name))
                        }
                    )
                    VerticalSpace(dp = 8.dp)
                    OutlinedTextField(
                        value = viewModel.editUrl.value,
                        onValueChange = {
                            viewModel.editUrl.value = it
                            if (viewModel.editUrlError.value.isNotEmpty()) {
                                viewModel.editUrlError.value = ""
                            }
                        },
                        singleLine = true,
                        label = {
                            Text(text = stringResource(id = R.string.url))
                        }
                    )
                    if (viewModel.editUrlError.value.isNotEmpty()) {
                        SelectionContainer {
                            Text(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                text = viewModel.editUrlError.value,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = viewModel.editUrl.value.isNotBlank() && viewModel.editName.value.isNotBlank(),
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.edit()
                    },
                ) {
                    Text(stringResource(id = R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.showEditDialog.value = false
                }) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            },
        )
    }
}