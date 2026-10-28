package com.ismartcoding.plain.ui.chat.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.features.chat.models.getMessagePreview
import com.ismartcoding.plain.ui.extensions.timeAgo
import com.ismartcoding.plain.models.ChatItem
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.theme.listItemSubtitle
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PeerListItem(
    modifier: Modifier = Modifier,
    title: String,
    desc: String,
    icon: DrawableResource,
    online: Boolean? = null,
    accessLabel: String? = null,
    latestChat: ChatItem? = null,
    peerId: String? = null,
    onDelete: ((String) -> Unit)? = null,
    onClick: () -> Unit = {},
) {
    val showContextMenu = remember { mutableStateOf(false) }
    val deleteDeviceText = stringResource(Res.string.delete_device)
    val deleteText = stringResource(Res.string.delete)
    val deleteWarningText = stringResource(Res.string.delete_peer_warning)
    val cancelText = stringResource(Res.string.cancel)
    val subtitle = latestChat?.getMessagePreview() ?: desc

    val onlineDescription = when (online) {
        true -> stringResource(Res.string.online)
        false -> stringResource(Res.string.offline)
        null -> null
    }
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
            minHeight = 80.dp,
            title = {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = title, modifier = Modifier.weight(1f))
                    latestChat?.let { chat ->
                        Text(
                            modifier = Modifier.padding(start = 8.dp, end = 8.dp),
                            text = chat.createdAt.timeAgo(),
                            style = MaterialTheme.typography.listItemSubtitle(),
                        )
                    }
                }
            },
            subtitle = if (subtitle.isEmpty()) null else { { Text(subtitle) } },
            description = accessLabel?.let { label ->
                { Text(listOfNotNull(label, onlineDescription).joinToString(" · ")) }
            },
            start = { PeerIconWithStatus(icon = icon, online = online) },
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
