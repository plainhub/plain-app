package com.ismartcoding.plain.httpserver.peerschemas

import com.ismartcoding.plain.enums.ChannelSystemMessageType
import com.ismartcoding.plain.lib.kgraphql.Context
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLMutation
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLSchemaTarget
import com.ismartcoding.plain.httpserver.models.ChatItem

// Contract declarations only. Runtime execution belongs to Rust.
@GraphQLMutation(
    target = GraphQLSchemaTarget.PEER,
    description = "Deliver a channel control message (invite / invite accept-decline / update / kick / leave) from the sending peer. `payload` is a channel-protocol JSON envelope interpreted per `type`.",
)
suspend fun channelSystemMessage(type: ChannelSystemMessageType, payload: String, context: Context): Boolean = error("Peer schema is for contract export only")

@GraphQLMutation(
    target = GraphQLSchemaTarget.PEER,
    description = "Deliver a chat message envelope from the sending peer (direct, or channel-bound via the c-cid header). Returns the stored item; a replayed message is silently dropped and returns an empty list.",
)
suspend fun createChatItem(content: String, context: Context): List<ChatItem> = error("Peer schema is for contract export only")

@GraphQLMutation(
    target = GraphQLSchemaTarget.PEER,
    description = "Ask this device to start the aware (LAN presence) channel and subscribe the sending peer. Returns whether the aware channel was started by this call.",
)
suspend fun startAware(context: Context): Boolean = error("Peer schema is for contract export only")
