package com.ismartcoding.plain.chat.channel

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("action")
internal sealed class ChannelCommand {
    @Serializable @SerialName("snapshot") data object Snapshot : ChannelCommand()
    @Serializable @SerialName("invitation") data class Invitation(val id: String, val owner: String?) : ChannelCommand()
    @Serializable @SerialName("create") data class Create(val name: String) : ChannelCommand()
    @Serializable @SerialName("rename") data class Rename(val id: String, val name: String) : ChannelCommand()
    @Serializable @SerialName("delete") data class Delete(val id: String) : ChannelCommand()
    @Serializable @SerialName("leave") data class Leave(val id: String) : ChannelCommand()
    @Serializable @SerialName("invite") data class Invite(val id: String, val peer: String) : ChannelCommand()
    @Serializable @SerialName("resend") data class Resend(val id: String, val peer: String) : ChannelCommand()
    @Serializable @SerialName("kick") data class Kick(val id: String, val peer: String) : ChannelCommand()
    @Serializable @SerialName("accept") data class Accept(val id: String) : ChannelCommand()
    @Serializable @SerialName("decline") data class Decline(val id: String) : ChannelCommand()
}
