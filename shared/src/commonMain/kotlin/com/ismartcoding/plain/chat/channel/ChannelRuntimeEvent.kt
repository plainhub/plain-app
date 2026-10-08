package com.ismartcoding.plain.chat.channel

import kotlinx.serialization.Serializable

@Serializable
internal data class ChannelRuntimeEvent(
    val changed: Boolean,
    val invite: Invite? = null,
    val cancel: Cancel? = null,
) {
    @Serializable data class Invite(val channelId: String, val ownerPeerId: String, val ownerPeerName: String)
    @Serializable data class Cancel(val channelId: String, val ownerPeerId: String)
}
