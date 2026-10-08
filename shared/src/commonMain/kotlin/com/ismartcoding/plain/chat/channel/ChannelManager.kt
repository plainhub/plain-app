package com.ismartcoding.plain.chat.channel

import com.ismartcoding.plain.chat.ChatManager
import com.ismartcoding.plain.chat.peer.GraphQLResponse
import com.ismartcoding.plain.db.DChatChannel
import com.ismartcoding.plain.lib.withIO
import kotlinx.serialization.json.JsonObject

object ChannelManager {
    private suspend fun mutate(command: ChannelCommand): JsonObject {
        val result = RustChannelRuntime.call(command)
        ChannelCacher.load()
        ChatManager.refreshLatestChats()
        return result
    }

    suspend fun createChannel(name: String): DChatChannel = withIO {
        RustChannelRuntime.channel(mutate(ChannelCommand.Create(name)))
    }
    suspend fun renameChannel(channelId: String, newName: String): DChatChannel = withIO {
        RustChannelRuntime.channel(mutate(ChannelCommand.Rename(channelId, newName)))
    }
    suspend fun deleteChannel(channelId: String) { withIO { mutate(ChannelCommand.Delete(channelId)) } }
    suspend fun leaveChannel(channelId: String) { withIO { mutate(ChannelCommand.Leave(channelId)) } }
    suspend fun inviteMember(channelId: String, peerId: String): DChatChannel = withIO {
        RustChannelRuntime.channel(mutate(ChannelCommand.Invite(channelId, peerId)))
    }
    suspend fun resendInvite(channelId: String, peerId: String) { withIO { mutate(ChannelCommand.Resend(channelId, peerId)) } }
    suspend fun kickMember(channelId: String, peerId: String): DChatChannel = withIO {
        RustChannelRuntime.channel(mutate(ChannelCommand.Kick(channelId, peerId)))
    }
    suspend fun acceptInvite(channelId: String): GraphQLResponse = withIO {
        RustChannelRuntime.response(mutate(ChannelCommand.Accept(channelId)))
    }
    suspend fun declineInvite(channelId: String) { withIO { mutate(ChannelCommand.Decline(channelId)) } }
}
