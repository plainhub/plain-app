package com.ismartcoding.plain.ui.chat.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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

    val onlineDescription = when (online) {
        true -> stringResource(Res.string.online)
        false -> stringResource(Res.string.offline)
        null -> null
    }
    Box {
        PListItem(
            modifier = modifier.heightIn(min = 80.dp).semantics {
                onlineDescription?.let { stateDescription = it }
            }.combinedClickable(
                onClick = onClick,
                onLongClick = {
                    if (peerId != null && onDelete != null) {
                        showContextMenu.value = true
                    }
                },
            ),
            title = title,
            subtitle = latestChat?.getMessagePreview() ?: desc,
            start = { PeerIconWithStatus(icon = icon, online = online) },
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
