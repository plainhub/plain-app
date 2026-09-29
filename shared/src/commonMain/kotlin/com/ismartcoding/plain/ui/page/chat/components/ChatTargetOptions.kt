package com.ismartcoding.plain.ui.page.chat.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.ismartcoding.plain.chat.channel.ChannelCacher
import com.ismartcoding.plain.chat.data.ChatTarget
import com.ismartcoding.plain.chat.data.ChatTargetType
import com.ismartcoding.plain.chat.peer.PeerCacher
import com.ismartcoding.plain.enums.getIcon
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.ui.resources.bot as ui_drawable_bot
import com.ismartcoding.plain.i18n.channel_members
import com.ismartcoding.plain.ui.resources.hash as ui_drawable_hash
import com.ismartcoding.plain.i18n.local_chat
import com.ismartcoding.plain.i18n.local_chat_desc
import com.ismartcoding.plain.ui.extensions.collectAsStateValue
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.bot as ui_drawable_bot
import com.ismartcoding.plain.ui.resources.hash as ui_drawable_hash

/**
 * A pickable chat destination for forwarding/selection UIs: local chat,
 * joined channels, then paired peers.
 */
data class ChatTargetOption(
    val target: ChatTarget,
    val title: String,
    val subtitle: String,
    val icon: DrawableResource,
)

@Composable
fun chatTargetOptions(): List<ChatTargetOption> {
    val pairedPeers = PeerCacher.pairedPeers.collectAsStateValue()
    val channels by ChannelCacher.channels.collectAsState()
    val localTitle = stringResource(Res.string.local_chat)
    val localDesc = stringResource(Res.string.local_chat_desc)
    val membersLabel = stringResource(Res.string.channel_members)
    return buildList {
        add(ChatTargetOption(ChatTarget("local", ChatTargetType.PEER), localTitle, localDesc, UiRes.drawable.ui_drawable_bot))
        channels.filter { it.isJoined() }.forEach { channel ->
            add(
                ChatTargetOption(
                    ChatTarget(channel.id, ChatTargetType.CHANNEL),
                    channel.name,
                    "${channel.joinedMembers().size} $membersLabel",
                    UiRes.drawable.ui_drawable_hash,
                ),
            )
        }
        pairedPeers.forEach { peer ->
            add(ChatTargetOption(ChatTarget(peer.id, ChatTargetType.PEER), peer.name, peer.ip, peer.deviceType.getIcon()))
        }
    }
}
