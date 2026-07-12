package com.ismartcoding.plain.web.schemas
import com.ismartcoding.plain.appContext

import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.chat.channel.ChannelManager
import com.ismartcoding.plain.platform.AppDatabase
import com.ismartcoding.plain.web.models.ChatChannel
import com.ismartcoding.plain.web.models.ChatChannelMember
import com.ismartcoding.plain.web.models.ID
import com.ismartcoding.plain.web.models.toModel

fun SchemaBuilder.addChatChannelSchema() {
    query("chatChannels") {
        resolver { ->
            AppDatabase.instance.chatChannelDao().getAll()
                .sortedBy { it.name.lowercase() }
                .map { it.toModel() }
        }
    }
    mutation("createChatChannel") {
        resolver("name") { name: String ->
            ChannelManager.createChannel(name).toModel()
        }
    }
    mutation("updateChatChannel") {
        resolver("id", "name") { id: ID, name: String ->
            ChannelManager.renameChannel(id.value, name).toModel()
        }
    }
    mutation("deleteChatChannel") {
        resolver("id") { id: ID ->
            ChannelManager.deleteChannel(id.value)
            true
        }
    }
    mutation("leaveChatChannel") {
        resolver("id") { id: ID ->
            ChannelManager.leaveChannel(id.value)
            true
        }
    }
    mutation("addChatChannelMember") {
        resolver("id", "peerId") { id: ID, peerId: String ->
            ChannelManager.inviteMember(id.value, peerId).toModel()
        }
    }
    mutation("removeChatChannelMember") {
        resolver("id", "peerId") { id: ID, peerId: String ->
            ChannelManager.kickMember(id.value, peerId).toModel()
        }
    }
    mutation("acceptChatChannelInvite") {
        resolver("id") { id: ID ->
            ChannelManager.acceptInvite(id.value)
            true
        }
    }
    mutation("declineChatChannelInvite") {
        resolver("id") { id: ID ->
            ChannelManager.declineInvite(id.value)
            true
        }
    }
    type<ChatChannel> {}
    type<ChatChannelMember> {}
}
