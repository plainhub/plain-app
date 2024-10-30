package com.ismartcoding.plain.ui.page.feeds

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.models.FeedsViewModel

@Composable
fun EditFeedDialog(feedsVM: FeedsViewModel) {
    if (feedsVM.showEditDialog.value) {
        val focusManager = LocalFocusManager.current
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surface,
            onDismissRequest = {
                feedsVM.showEditDialog.value = false
            },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.square_pen),
                    contentDescription = stringResource(id = R.string.edit),
                )
            },
            title = {
                Text(
                    text = stringResource(id = R.string.edit), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = feedsVM.editName.value,
                        onValueChange = {
                            feedsVM.editName.value = it
                        },
                        singleLine = true,
                        label = {
                            Text(text = stringResource(id = R.string.name))
                        }
                    )
                    VerticalSpace(dp = 8.dp)
                    OutlinedTextField(
                        value = feedsVM.editUrl.value,
                        onValueChange = {
                            feedsVM.editUrl.value = it
                            if (feedsVM.editUrlError.value.isNotEmpty()) {
                                feedsVM.editUrlError.value = ""
                            }
                        },
                        singleLine = true,
                        label = {
                            Text(text = stringResource(id = R.string.url))
                        }
                    )
                    if (feedsVM.editUrlError.value.isNotEmpty()) {
                        SelectionContainer {
                            Text(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                text = feedsVM.editUrlError.value,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = feedsVM.editUrl.value.isNotBlank() && feedsVM.editName.value.isNotBlank(),
                    onClick = {
                        focusManager.clearFocus()
                        feedsVM.edit()
                    },
                ) {
                    Text(stringResource(id = R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    feedsVM.showEditDialog.value = false
                }) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            },
        )
    }
}