package com.ismartcoding.plain.ui.page.chat.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.extensions.timeAgo
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.theme.listItemSubtitle
import com.ismartcoding.plain.ui.theme.listItemTitle

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PeerListItem(
    modifier: Modifier = Modifier,
    title: String,
    desc: String,
    icon: Int,
    online: Boolean? = null,
    latestChat: DChat? = null,
    peerId: String? = null,
    onDelete: ((String) -> Unit)? = null,
    onClick: () -> Unit = {},
) {
    val showContextMenu = remember { mutableStateOf(false) }
    val deleteDeviceText = stringResource(id = R.string.delete_device)
    val deleteText = stringResource(id = R.string.delete)
    val deleteWarningText = stringResource(id = R.string.delete_peer_warning)
    val cancelText = stringResource(id = R.string.cancel)

    Surface(
        modifier = modifier.combinedClickable(
            onClick = onClick,
            onLongClick = {
                if (peerId != null && onDelete != null) {
                    showContextMenu.value = true
                }
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
                PeerIconWithStatus(icon = icon, title = title, online = online)
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
                            text = title,
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
                        text = latestChat?.getMessagePreview() ?: desc,
                        style = MaterialTheme.typography.listItemSubtitle(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (peerId != null && onDelete != null) {
                PeerContextMenu(
                    peerId = peerId,
                    showContextMenu = showContextMenu,
                    deleteDeviceText = deleteDeviceText,
                    deleteText = deleteText,
                    deleteWarningText = deleteWarningText,
                    cancelText = cancelText,
                    onDelete = onDelete,
                )
            }
        }
    }
}

