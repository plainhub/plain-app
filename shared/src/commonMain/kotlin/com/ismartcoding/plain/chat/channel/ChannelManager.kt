package com.ismartcoding.plain.chat.channel


import com.ismartcoding.plain.chat.ChatManager
import com.ismartcoding.plain.chat.peer.GraphQLResponse
import com.ismartcoding.plain.db.DChatChannel
import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.lib.coIO
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.httpserver.models.toModel

import kotlinx.serialization.json.put

object ChannelManager {

    init {
        startChannelBroadcaster()
    }

    private fun startChannelBroadcaster() {
        coIO {
            ChannelCacher.channels
                .collect { channels ->
                    sendEvent(
                        WebSocketEvent(
                            EventType.CHANNELS_UPDATED,
                            channelsToJsonModelString(channels),
                        ),
                    )
                    ChatManager.refreshLatestChats()
                }
        }
    }

    private suspend fun mutate(action: String, id: String? = null, fields: kotlinx.serialization.json.JsonObjectBuilder.() -> Unit = {}): kotlinx.serialization.json.JsonObject {
        val result = RustChannelRuntime.call(action, id, fields)
        ChannelCacher.load()
        ChatManager.refreshLatestChats()
        return result
    }

    suspend fun createChannel(name: String): DChatChannel = withIO {
        RustChannelRuntime.channel(mutate("create") { put("name", name) })
    }
    suspend fun renameChannel(channelId: String, newName: String): DChatChannel = withIO {
        RustChannelRuntime.channel(mutate("rename", channelId) { put("name", newName) })
    }
    suspend fun deleteChannel(channelId: String) { withIO { mutate("delete", channelId) } }
    suspend fun leaveChannel(channelId: String) { withIO { mutate("leave", channelId) } }
    suspend fun inviteMember(channelId: String, peerId: String): DChatChannel = withIO {
        RustChannelRuntime.channel(mutate("invite", channelId) { put("peer", peerId) })
    }
    suspend fun resendInvite(channelId: String, peerId: String) { withIO { mutate("resend", channelId) { put("peer", peerId) } } }
    suspend fun kickMember(channelId: String, peerId: String): DChatChannel = withIO {
        RustChannelRuntime.channel(mutate("kick", channelId) { put("peer", peerId) })
    }
    suspend fun acceptInvite(channelId: String): GraphQLResponse = withIO {
        RustChannelRuntime.response(mutate("accept", channelId))
    }
    suspend fun declineInvite(channelId: String) { withIO { mutate("decline", channelId) } }
}

private fun channelsToJsonModelString(channels: List<DChatChannel>): String =
    JsonHelper.jsonEncode(channels.map { it.toModel() })
