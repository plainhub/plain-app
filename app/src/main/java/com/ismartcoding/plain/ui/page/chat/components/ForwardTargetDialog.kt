package com.ismartcoding.plain.ui.page.chat.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.chat.channel.ChannelCacher
import com.ismartcoding.plain.chat.data.ChatTarget
import com.ismartcoding.plain.chat.data.ChatTargetType
import com.ismartcoding.plain.chat.peer.PeerCacher
import com.ismartcoding.plain.enums.DeviceType
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.bot
import com.ismartcoding.plain.i18n.channel_members
import com.ismartcoding.plain.i18n.forward
import com.ismartcoding.plain.i18n.hash
import com.ismartcoding.plain.i18n.local_chat
import com.ismartcoding.plain.i18n.local_chat_desc
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PBottomSheetTopAppBar
import com.ismartcoding.plain.ui.base.PListItem
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import com.ismartcoding.plain.ui.models.ChannelViewModel
import com.ismartcoding.plain.ui.models.PeerViewModel
import com.ismartcoding.plain.ui.extensions.collectAsStateValue
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForwardTargetDialog(
    peerVM: PeerViewModel,
    channelVM: ChannelViewModel,
    onDismiss: () -> Unit,
    onTargetSelected: (ChatTarget) -> Unit
) {
    val pairedPeers = PeerCacher.pairedPeers.collectAsStateValue()
    val channels by ChannelCacher.channels.collectAsState()
    val joinedChannels = channels.filter { it.isJoined() }

    PModalBottomSheet(onDismissRequest = onDismiss) {
        PBottomSheetTopAppBar(title = stringResource(Res.string.forward))
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            item {
                PListItem(
                    modifier = Modifier.clickable {
                        onTargetSelected(ChatTarget("local", ChatTargetType.PEER))
                        onDismiss()
                    },
                    title = stringResource(Res.string.local_chat),
                    subtitle = stringResource(Res.string.local_chat_desc),
                    start = { TargetIcon(Res.drawable.bot) },
                    showMore = true,
                )
            }
            if (joinedChannels.isNotEmpty()) {
                items(joinedChannels, key = { it.id }) { channel ->
                    PListItem(
                        modifier = Modifier.clickable {
                            onTargetSelected(ChatTarget(channel.id, ChatTargetType.CHANNEL))
                            onDismiss()
                        },
                        title = channel.name,
                        subtitle = "${channel.joinedMembers().size} ${stringResource(Res.string.channel_members)}",
                        start = { TargetIcon(Res.drawable.hash) },
                        showMore = true,
                    )
                }
            }
            if (pairedPeers.isNotEmpty()) {
                items(pairedPeers, key = { it.id }) { peer ->
                    PListItem(
                        modifier = Modifier.clickable {
                            onTargetSelected(ChatTarget(peer.id, ChatTargetType.PEER))
                            onDismiss()
                        },
                        title = peer.name,
                        subtitle = peer.ip,
                        start = { TargetIcon(DeviceType.fromValue(peer.deviceType).getIcon()) },
                        showMore = true,
                    )
                }
            }
            item { BottomSpace() }
        }
    }
}

@Composable
private fun TargetIcon(icon: DrawableResource) {
    Box(
        modifier = Modifier.size(56.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}