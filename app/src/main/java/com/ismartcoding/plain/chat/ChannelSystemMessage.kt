package com.ismartcoding.plain.chat

import com.ismartcoding.plain.db.ChannelMember
import kotlinx.serialization.Serializable

/**
 * System messages used for channel management (invite, accept, update, kick, leave).
 * These are sent between peers via the PeerGraphQL transport layer.
 *
 * The [type] string is used as a discriminator when serializing/deserializing
 * so that the receiver can route to the correct handler.
 */
object ChannelSystemMessages {

    // ── Type constants ─────────────────────────────────────────────
    const val TYPE_INVITE = "channel_invite"
    const val TYPE_INVITE_ACCEPT = "channel_invite_accept"
    const val TYPE_INVITE_DECLINE = "channel_invite_decline"
    const val TYPE_UPDATE = "channel_update"
    const val TYPE_KICK = "channel_kick"
    const val TYPE_LEAVE = "channel_leave"

    // ── Message payloads ───────────────────────────────────────────

    /** Owner → Invitee: sent when a peer is invited to the channel.
     *  The channel key is sent in plaintext because the PeerGraphQL transport
     *  already encrypts the entire request with the peer's shared ChaCha20 key
     *  and verifies the Ed25519 signature.
     *
     *  The invitee should create a peer record (status="channel") for each member
     *  it doesn't already have in its local peers table, using the peer info
     *  carried in [memberPeers]. */
    @Serializable
    data class ChannelInvite(
        val channelId: String,
        val channelName: String,
        /** Base64-encoded symmetric ChaCha20 key for the channel. */
        val key: String,
        val owner: String,
        val members: List<ChannelMember>,
        /** Lightweight peer info for members, so that the invitee can create
         *  peer records for members it doesn't already know. */
        val memberPeers: List<MemberPeerInfo> = emptyList(),
        val version: Long,
    )

    /** Lightweight peer info included in ChannelInvite so the receiver can
     *  create peer records for channel members it doesn't have locally. */
    @Serializable
    data class MemberPeerInfo(
        val id: String,
        val name: String = "",
        val publicKey: String = "",
        val deviceType: String = "",
        val ip: String = "",
        val port: Int = 0,
    )

    /** Invitee → Owner: invitation accepted. Includes the accepter's public key
     *  so the owner can store it in the peer record. */
    @Serializable
    data class ChannelInviteAccept(
        val channelId: String,
        val publicKey: String = "",
        val name: String = "",
        val deviceType: String = "",
    )

    /** Invitee → Owner: invitation declined. */
    @Serializable
    data class ChannelInviteDecline(
        val channelId: String,
    )

    /** Owner → All members (including pending): channel metadata changed
     *  (rename, member added/removed, etc.).
     *  Members list only carries id + status; peer details are in the peers table. */
    @Serializable
    data class ChannelUpdate(
        val channelId: String,
        val channelName: String,
        val members: List<ChannelMember>,
        /** Lightweight peer info for any new members added since last update. */
        val memberPeers: List<MemberPeerInfo> = emptyList(),
        val version: Long,
    )

    /** Owner → Kicked peer: you have been removed from the channel. */
    @Serializable
    data class ChannelKick(
        val channelId: String,
    )

    /** Member → Owner: the sender is voluntarily leaving the channel. */
    @Serializable
    data class ChannelLeave(
        val channelId: String,
    )
}
