package com.ismartcoding.plain.ui.page.chat.components

import com.ismartcoding.plain.i18n.*

import org.jetbrains.compose.resources.DrawableResource
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.db.getMessagePreview
import com.ismartcoding.plain.extensions.timeAgo
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.theme.listItemSubtitle

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PeerListItem(
    modifier: Modifier = Modifier,
    title: String,
    desc: String,
    icon: DrawableResource,
    online: Boolean? = null,
    latestChat: DChat? = null,
    peerId: String? = null,
    onDelete: ((String) -> Unit)? = null,
    onClick: () -> Unit = {},
) {
    val showContextMenu = remember { mutableStateOf(false) }
    val deleteDeviceText = stringResource(Res.string.delete_device)
    val deleteText = stringResource(Res.string.delete)
    val deleteWarningText = stringResource(Res.string.delete_peer_warning)
    val cancelText = stringResource(Res.string.cancel)

    Box {
        PListItem(
            modifier = modifier.combinedClickable(
                onClick = onClick,
                onLongClick = {
                    if (peerId != null && onDelete != null) {
                        showContextMenu.value = true
                    }
                },
            ),
            title = title,
            subtitle = latestChat?.getMessagePreview() ?: desc,
            start = { PeerIconWithStatus(icon = icon, title = title, online = online) },
            titleTrailing = latestChat?.let { chat ->
                {
                    Text(
                        modifier = Modifier.padding(end = 8.dp),
                        text = chat.createdAt.timeAgo(),
                        style = MaterialTheme.typography.listItemSubtitle()
                    )
                }
            },
        )

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

