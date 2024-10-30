package com.ismartcoding.plain.ui.page.feeds

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
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
fun AddFeedDialog(feedsVM: FeedsViewModel) {
    if (feedsVM.showAddDialog.value) {
        val focusManager = LocalFocusManager.current
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surface,
            onDismissRequest = {
                feedsVM.showAddDialog.value = false
            },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.rss),
                    contentDescription = stringResource(id = R.string.subscriptions),
                )
            },
            title = {
                Text(text = stringResource(id = R.string.add_subscription), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                if (feedsVM.rssChannel.value == null) {
                    ClipboardTextField(
                        value = feedsVM.editUrl.value,
                        onValueChange = {
                            feedsVM.editUrl.value = it
                            if (feedsVM.editUrlError.value.isNotEmpty()) {
                                feedsVM.editUrlError.value = ""
                            }
                        },
                        placeholder = stringResource(id = R.string.rss_url),
                        errorText = if (feedsVM.editUrl.value.isNotEmpty()) feedsVM.editUrlError.value else "",
                        focusManager = focusManager,
                        requestFocus = true,
                        onConfirm = {
                            feedsVM.fetchChannel()
                        },
                    )
                } else {
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
                        PDialogListItem(
                            title = stringResource(id = R.string.auto_fetch_full_content),
                        ) {
                            PSwitch(
                                activated = feedsVM.editFetchContent.value,
                            ) {
                                feedsVM.editFetchContent.value = it
                            }
                        }
                        PDialogTips(text = stringResource(id = R.string.auto_fetch_full_content_tips))
                    }
                }
            },
            confirmButton = {
                val buttonText = if (feedsVM.rssChannel.value == null) R.string.search else R.string.add
                Button(
                    enabled = feedsVM.editUrl.value.isNotBlank(),
                    onClick = {
                        focusManager.clearFocus()
                        if (feedsVM.rssChannel.value == null) {
                            feedsVM.fetchChannel()
                        } else {
                            feedsVM.add()
                        }
                    },
                ) {
                    Text(stringResource(id = buttonText))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    feedsVM.showAddDialog.value = false
                }) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            },
        )
    }
}