package com.ismartcoding.plain.httpserver.peerschemas

import com.ismartcoding.plain.chat.ChatMessageReceiver
import com.ismartcoding.plain.chat.ReplayedMessageException
import com.ismartcoding.plain.chat.channel.ChannelSystemMessageReceiver
import com.ismartcoding.plain.enums.ChannelSystemMessageType
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.lib.kgraphql.Context
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLMutation
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLSchemaTarget
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.platform.AppDatabase
import com.ismartcoding.plain.platform.startAwareIfNeeded
import com.ismartcoding.plain.platform.subscribeAwareForPeer
import com.ismartcoding.plain.httpserver.PeerGraphQLService
import com.ismartcoding.plain.httpserver.http.GraphqlRequestContext
import com.ismartcoding.plain.httpserver.models.ChatItem
import com.ismartcoding.plain.httpserver.models.toModel


@GraphQLMutation(
    target = GraphQLSchemaTarget.PEER,
    description = "Deliver a channel control message (invite / invite accept-decline / update / kick / leave) from the sending peer. `payload` is a channel-protocol JSON envelope interpreted per `type`.",
)
suspend fun channelSystemMessage(type: ChannelSystemMessageType, payload: String, context: Context): Boolean {
    val ctx = context.get<GraphqlRequestContext>()!!
    val fromId = ctx.header("c-id") ?: ""
    ChannelSystemMessageReceiver.handle(fromId, type, payload)
    return true
}

@GraphQLMutation(
    target = GraphQLSchemaTarget.PEER,
    description = "Deliver a chat message envelope from the sending peer (direct, or channel-bound via the c-cid header). Returns the stored item; a replayed message is silently dropped and returns an empty list.",
)
suspend fun createChatItem(content: String, context: Context): List<ChatItem> {
    val ctx = context.get<GraphqlRequestContext>()!!
    val fromPeerId = ctx.header("c-id") ?: ""
    val fromChannelId = ctx.header("c-cid") ?: ""
    val signature: String = ctx.attribute(PeerGraphQLService.ATTR_SIGNATURE) ?: ""
    val timestamp: Long = ctx.attribute(PeerGraphQLService.ATTR_TIMESTAMP) ?: 0L

    val item = try {
        ChatMessageReceiver.receive(
            fromPeerId = fromPeerId,
            content = DChat.parseContent(content),
            fromChannelId = fromChannelId,
            signature = signature,
            timestamp = timestamp,
        )
    } catch (e: ReplayedMessageException) {
        LogCat.d("Dropped replayed message from $fromPeerId")
        return emptyList()
    }
    return listOf(item.toModel())
}

@GraphQLMutation(
    target = GraphQLSchemaTarget.PEER,
    description = "Ask this device to start the aware (LAN presence) channel and subscribe the sending peer. Returns whether the aware channel was started by this call.",
)
suspend fun startAware(context: Context): Boolean {
    val ctx = context.get<GraphqlRequestContext>()!!
    val fromPeerId = ctx.header("c-id") ?: ""
    LogCat.d("[PeerGraphQL] startAware from=$fromPeerId")
    val started = startAwareIfNeeded()
    if (started) {
        withIO {
            val peer = AppDatabase.instance.peerDao().getById(fromPeerId)
            if (peer != null) {
                subscribeAwareForPeer(peer)
            }
        }
    }
    return started
}
