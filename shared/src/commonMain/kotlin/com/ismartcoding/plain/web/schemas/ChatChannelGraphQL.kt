package com.ismartcoding.plain.web.schemas

import com.ismartcoding.plain.chat.channel.ChannelCacher
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLMutation
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLQuery
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.chat.channel.ChannelManager
import com.ismartcoding.plain.lib.extensions.toSortName
import com.ismartcoding.plain.platform.AppDatabase
import com.ismartcoding.plain.web.models.ChatChannel
import com.ismartcoding.plain.web.models.ChatChannelMember
import com.ismartcoding.plain.web.models.ID
import com.ismartcoding.plain.web.models.toModel

@GraphQLQuery
suspend fun chatChannels(): List<ChatChannel> {
    return ChannelCacher.channels.value
        .sortedBy { it.name.toSortName() }
        .map { it.toModel() }
}

@GraphQLMutation
suspend fun createChatChannel(name: String): ChatChannel {
    return ChannelManager.createChannel(name).toModel()
}

@GraphQLMutation
suspend fun updateChatChannel(id: ID, name: String): ChatChannel {
    return ChannelManager.renameChannel(id.value, name).toModel()
}

@GraphQLMutation
suspend fun deleteChatChannel(id: ID): Boolean {
    ChannelManager.deleteChannel(id.value)
    return true
}

@GraphQLMutation
suspend fun leaveChatChannel(id: ID): Boolean {
    ChannelManager.leaveChannel(id.value)
    return true
}

@GraphQLMutation
suspend fun addChatChannelMember(id: ID, peerId: String): ChatChannel {
    return ChannelManager.inviteMember(id.value, peerId).toModel()
}

@GraphQLMutation
suspend fun removeChatChannelMember(id: ID, peerId: String): ChatChannel {
    return ChannelManager.kickMember(id.value, peerId).toModel()
}

@GraphQLMutation
suspend fun acceptChatChannelInvite(id: ID): Boolean {
    ChannelManager.acceptInvite(id.value)
    return true
}

@GraphQLMutation
suspend fun declineChatChannelInvite(id: ID): Boolean {
    ChannelManager.declineInvite(id.value)
    return true
}

fun SchemaBuilder.addChatChannelSchema() {
    type<ChatChannel> {}
    type<ChatChannelMember> {}
}
