package com.ismartcoding.plain.chat.channel

import com.ismartcoding.plain.db.ChannelMember
import kotlinx.serialization.Serializable

object ChannelSystemMessages {

    // ── Type constants ─────────────────────────────────────────────
    const val TYPE_INVITE = "channel_invite"
    const val TYPE_INVITE_ACCEPT = "channel_invite_accept"
    const val TYPE_INVITE_DECLINE = "channel_invite_decline"
    const val TYPE_UPDATE = "channel_update"
    const val TYPE_KICK = "channel_kick"
    const val TYPE_LEAVE = "channel_leave"

    // ── Action constants (signature payload) ───────────────────────
    const val ACTION_INVITE = "invite"
    const val ACTION_UPDATE = "update"
    const val ACTION_KICK = "kick"

    @Serializable
    data class ChannelInvite(
        val channelId: String,
        val channelName: String,
        /** Base64-encoded symmetric ChaCha20 key for the channel. */
        val key: String,
        val owner: String,
        val members: List<ChannelMember>,
        /** Lightweight peer info for members, so that the invitee can create
         *  peer records for members it doesn't already have. The owner's
         *  publicKey is taken from the entry whose id matches [owner]. */
        val memberPeers: List<MemberPeerInfo> = emptyList(),
        val version: Long,
        /** Ed25519 signature of `"$channelId|$version|invite|<invitee peer id>"`
         *  (Base64), signed by the owner at send time. */
        val signature: String = "",
    )

    @Serializable
    data class MemberPeerInfo(
        val id: String,
        val name: String = "",
        val publicKey: String = "",
        val deviceType: String = "",
        val ip: String = "",
        val port: Int = 0,
    )

    @Serializable
    data class ChannelInviteAccept(
        val channelId: String,
        val publicKey: String = "",
        val name: String = "",
        val deviceType: String = "",
    )

    @Serializable
    data class ChannelInviteDecline(
        val channelId: String,
    )

    @Serializable
    data class ChannelUpdate(
        val channelId: String,
        val channelName: String,
        val members: List<ChannelMember>,
        /** Lightweight peer info for any new members added since last update. */
        val memberPeers: List<MemberPeerInfo> = emptyList(),
        val version: Long,
        /** Ed25519 signature of `"$channelId|$version|update|"` (Base64),
         *  signed by the owner at send time. */
        val signature: String = "",
    )

    @Serializable
    data class ChannelKick(
        val channelId: String,
        /** Channel version at time of kick — included in the signature payload
         *  to bind the kick to a specific channel state. */
        val version: Long = 0,
        /** Ed25519 signature of `"$channelId|$version|kick|<kicked peer id>"`
         *  (Base64), signed by the owner at send time. */
        val signature: String = "",
    )

    /** Member → Owner: the sender is voluntarily leaving the channel. */
    @Serializable
    data class ChannelLeave(
        val channelId: String,
    )
}

fun channelMessagePayload(
    channelId: String,
    version: Long,
    action: String,
    target: String,
): String = "$channelId|$version|$action|$target"