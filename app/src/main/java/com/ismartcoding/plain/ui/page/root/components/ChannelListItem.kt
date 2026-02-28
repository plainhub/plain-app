package com.ismartcoding.plain.ui.page.root.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.extensions.timeAgo
import com.ismartcoding.plain.ui.base.PDropdownMenu
import com.ismartcoding.plain.ui.base.PDropdownMenuItem
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.theme.listItemSubtitle
import com.ismartcoding.plain.ui.theme.listItemTitle

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChannelListItem(
    modifier: Modifier = Modifier,
    name: String,
    channelId: String,
    latestChat: DChat? = null,
    onDelete: ((String) -> Unit)? = null,
    onRename: ((String) -> Unit)? = null,
    onManageMembers: ((String) -> Unit)? = null,
    onClick: () -> Unit = {},
) {
    val showContextMenu = remember { mutableStateOf(false) }
    val deleteText = stringResource(id = R.string.delete)
    val deleteWarningText = stringResource(id = R.string.delete_channel_warning)
    val renameText = stringResource(id = R.string.rename)
    val cancelText = stringResource(id = R.string.cancel)
    val manageMembersText = stringResource(id = R.string.manage_members)

    Surface(
        modifier = modifier.combinedClickable(
            onClick = onClick,
            onLongClick = {
                showContextMenu.value = true
            }
        ),
        color = Color.Unspecified,
    ) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp, 8.dp, 8.dp, 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(40.dp)
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.hash),
                        contentDescription = name,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.listItemTitle(),
                            modifier = Modifier.weight(1f)
                        )
                        latestChat?.let { chat ->
                            Text(
                                text = chat.createdAt.timeAgo(),
                                style = MaterialTheme.typography.listItemSubtitle(),
                            )
                        }
                    }
                    VerticalSpace(dp = 8.dp)
                    Text(
                        text = latestChat?.getMessagePreview() ?: "",
                        style = MaterialTheme.typography.listItemSubtitle(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Context menu
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 32.dp)
                    .wrapContentSize(Alignment.Center),
            ) {
                PDropdownMenu(
                    expanded = showContextMenu.value,
                    onDismissRequest = {
                        showContextMenu.value = false
                    },
                ) {
                    if (onManageMembers != null) {
                        PDropdownMenuItem(
                            text = { Text(manageMembersText) },
                            onClick = {
                                showContextMenu.value = false
                                onManageMembers(channelId)
                            },
                        )
                    }
                    if (onRename != null) {
                        PDropdownMenuItem(
                            text = { Text(renameText) },
                            onClick = {
                                showContextMenu.value = false
                                onRename(channelId)
                            },
                        )
                    }
                    if (onDelete != null) {
                        PDropdownMenuItem(
                            text = { Text(deleteText) },
                            onClick = {
                                showContextMenu.value = false
                                DialogHelper.showConfirmDialog(
                                    title = deleteText,
                                    message = deleteWarningText,
                                    confirmButton = Pair(deleteText) {
                                        onDelete(channelId)
                                    },
                                    dismissButton = Pair(cancelText) {}
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}
