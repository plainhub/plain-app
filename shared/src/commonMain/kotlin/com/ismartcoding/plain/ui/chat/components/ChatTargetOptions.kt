package com.ismartcoding.plain.ui.chat.components

import com.ismartcoding.plain.ui.chat.isJoined
import com.ismartcoding.plain.ui.chat.joinedMembers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.ismartcoding.plain.rust.graphql.ContentSource
import com.ismartcoding.plain.rust.graphql.GraphqlOperation
import com.ismartcoding.plain.rust.graphql.SubscriptionUpdate
import com.ismartcoding.plain.ui.components.DialogHelper
import com.ismartcoding.plain.i18n.load_failed
import com.ismartcoding.plain.lib.logcat.LogCat
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.catch
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ismartcoding.plain.features.chat.data.ChatTarget
import com.ismartcoding.plain.features.chat.data.ChatTargetType
import com.ismartcoding.plain.ui.chat.PeerViewModel
import com.ismartcoding.plain.ui.chat.ChannelViewModel
import com.ismartcoding.plain.ui.chat.sortChatChannels
import com.ismartcoding.plain.ui.platform.system.getIcon
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.ui.resources.bot as ui_drawable_bot
import com.ismartcoding.plain.i18n.channel_members
import com.ismartcoding.plain.ui.resources.hash as ui_drawable_hash
import com.ismartcoding.plain.i18n.local_chat
import com.ismartcoding.plain.i18n.local_chat_desc
import com.ismartcoding.plain.ui.media.extensions.collectAsStateValue
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.ui.resources.Res as UiRes

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
    val source = com.ismartcoding.plain.ui.navigation.LocalContentSource.current
    val changes = remember(source) {
        if (source is ContentSource.Remote) {
            val fields = listOf("contentChanged", "messagesCreated { id }", "messagesUpdated { id }", "messagesDeleted", "peersChanged", "channelsChanged { changed }")
            source.client.subscriptionUpdates(fields.map { GraphqlOperation("subscription { $it }") }).transform { update ->
                when (update) {
                    is SubscriptionUpdate.Failed -> throw update.error
                    else -> emit(Unit)
                }
            }.catch {
                LogCat.e("Chat destination updates", it)
                DialogHelper.showMessage(Res.string.load_failed)
            }
        } else null
    }
    val peerVM: PeerViewModel = viewModel(key = "chatTargetPeers:${source.key}") {
        PeerViewModel(
            readPeers = { source.api.execute(com.ismartcoding.plain.graphql.Query.peerSummaries()) },
            readLatestChats = { source.api.execute(com.ismartcoding.plain.graphql.Query.chatConversations()).associate { it.id to it.chat } },
            sourceChanges = changes,
            api = source.api,
        )
    }
    val channelVM: ChannelViewModel = viewModel(key = "chatTargetChannels:${source.key}") {
        ChannelViewModel(api = source.api, sourceChanges = peerVM.sourceUpdates)
    }
    val peers = peerVM.peerList.collectAsStateValue()
    val latestChats by peerVM.latestChats.collectAsState()
    LaunchedEffect(peerVM) { peerVM.observe() }
    LaunchedEffect(channelVM) { channelVM.observe() }
    val channels by channelVM.channels.collectAsState()
    val localTitle = stringResource(Res.string.local_chat)
    val localDesc = stringResource(Res.string.local_chat_desc)
    val membersLabel = stringResource(Res.string.channel_members)
    return buildList {
        add(ChatTargetOption(ChatTarget("local", ChatTargetType.PEER), localTitle, localDesc, UiRes.drawable.ui_drawable_bot))
        sortChatChannels(channels, latestChats).filter { it.isJoined() }.forEach { channel ->
            add(
                ChatTargetOption(
                    ChatTarget(channel.id, ChatTargetType.CHANNEL),
                    channel.name,
                    "${channel.joinedMembers().size} $membersLabel",
                    UiRes.drawable.ui_drawable_hash,
                ),
            )
        }
        peers.filter { it.isPaired() }.forEach { peer ->
            add(ChatTargetOption(ChatTarget(peer.id, ChatTargetType.PEER), peer.name, peer.ip, peer.deviceType.getIcon()))
        }
    }
}
