package com.ismartcoding.plain.ui.page.feeds

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RssFeed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.base.ClipboardTextField
import com.ismartcoding.plain.ui.base.PDialogListItem
import com.ismartcoding.plain.ui.base.PDialogTips
import com.ismartcoding.plain.ui.base.PSwitch
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.models.FeedsViewModel

@Composable
fun AddFeedDialog(viewModel: FeedsViewModel) {
    if (viewModel.showAddDialog.value) {
        val focusManager = LocalFocusManager.current
        AlertDialog(
            onDismissRequest = {
                viewModel.showAddDialog.value = false
            },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.RssFeed,
                    contentDescription = stringResource(id = R.string.subscriptions),
                )
            },
            title = {
                Text(text = stringResource(id = R.string.add_subscription), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                if (viewModel.rssChannel.value == null) {
                    ClipboardTextField(
                        value = viewModel.editUrl.value,
                        onValueChange = {
                            viewModel.editUrl.value = it
                            if (viewModel.editUrlError.value.isNotEmpty()) {
                                viewModel.editUrlError.value = ""
                            }
                        },
                        placeholder = stringResource(id = R.string.rss_url),
                        errorText = if (viewModel.editUrl.value.isNotEmpty()) viewModel.editUrlError.value else "",
                        focusManager = focusManager,
                        requestFocus = true,
                        onConfirm = {
                            viewModel.fetchChannel()
                        },
                    )
                } else {
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
                        PDialogListItem(
                            title = stringResource(id = R.string.auto_fetch_full_content),
                        ) {
                            PSwitch(
                                activated = viewModel.editFetchContent.value,
                            ) {
                                viewModel.editFetchContent.value = it
                            }
                        }
                        PDialogTips(text = stringResource(id = R.string.auto_fetch_full_content_tips))
                    }
                }
            },
            confirmButton = {
                val buttonText = if (viewModel.rssChannel.value == null) R.string.search else R.string.add
                Button(
                    enabled = viewModel.editUrl.value.isNotBlank(),
                    onClick = {
                        focusManager.clearFocus()
                        if (viewModel.rssChannel.value == null) {
                            viewModel.fetchChannel()
                        } else {
                            viewModel.add()
                        }
                    },
                ) {
                    Text(stringResource(id = buttonText))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.showAddDialog.value = false
                }) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            },
        )
    }
}