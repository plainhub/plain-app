package com.ismartcoding.plain.ui.page.feeds

import com.ismartcoding.plain.i18n.*
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import com.ismartcoding.plain.ui.base.PTextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalFocusManager
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.enums.ButtonSize
import com.ismartcoding.plain.ui.base.ClipboardTextField
import com.ismartcoding.plain.ui.base.PDialogListItem
import com.ismartcoding.plain.ui.base.PDialogTips
import com.ismartcoding.plain.ui.base.PFilledButton
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
                    painter = painterResource(Res.drawable.rss),
                    contentDescription = stringResource(Res.string.subscriptions),
                )
            },
            title = {
                Text(text = stringResource(Res.string.add_subscription), maxLines = 1, overflow = TextOverflow.Ellipsis,
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
                        placeholder = stringResource(Res.string.rss_url),
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
                                Text(text = stringResource(Res.string.name))
                            }
                        )
                        VerticalSpace(dp = 8.dp)
                        PDialogListItem(
                            title = stringResource(Res.string.auto_fetch_full_content),
                        ) {
                            PSwitch(
                                activated = feedsVM.editFetchContent.value,
                            ) {
                                feedsVM.editFetchContent.value = it
                            }
                        }
                        PDialogTips(text = stringResource(Res.string.auto_fetch_full_content_tips))
                    }
                }
            },
            confirmButton = {
                val buttonText = if (feedsVM.rssChannel.value == null) Res.string.search else Res.string.add
                PFilledButton(
                    text = stringResource(buttonText),
                    buttonSize = ButtonSize.MEDIUM,
                    enabled = feedsVM.editUrl.value.isNotBlank(),
                    onClick = {
                        focusManager.clearFocus()
                        if (feedsVM.rssChannel.value == null) {
                            feedsVM.fetchChannel()
                        } else {
                            feedsVM.add()
                        }
                    },
                )
            },
            dismissButton = {
                PTextButton(text = stringResource(Res.string.cancel), onClick = { feedsVM.showAddDialog.value = false })
            },
        )
    }
}