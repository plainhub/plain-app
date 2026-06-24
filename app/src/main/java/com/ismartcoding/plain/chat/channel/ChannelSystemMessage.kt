package com.ismartcoding.plain.chat.channel

import com.ismartcoding.plain.db.ChannelMember
import kotlinx.serialization.Serializable

/**
 * System messages used for channel management (invite, accept, update, kick, leave).
 * These are sent between peers via the PeerGraphQL transport layer.
 *
 * The [type] string is used as a discriminator when serializing/deserializing
 * so that the receiver can route to the correct handler.
 *
 * ## Owner authentication
 *
 * Every owner-originated message carries an Ed25519 [signature] of
 * `"$channelId|$version|$action|$target"`, signed with the owner's private key
 * at send time. The receiver verifies the signature against the owner's public
 * key (sourced from the owner's DPeer row, populated via [memberPeers] for
 * `ChannelInvite` and locally cached for `ChannelUpdate`/`ChannelKick`).
 *
 * - [channelId] + [version] bind the action to a specific channel state, so an
 *   attacker can't replay an old invite/update to resurrect a stale version.
 * - [action] disambiguates the message type so a stolen signature can't be
 *   repurposed across invite/update/kick.
 * - [target] binds the message to a specific recipient for invite/kick, so
 *   peer A can't replay an invite/kick meant for peer B.
 */
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

    // ── Message payloads ───────────────────────────────────────────

    /** Owner → Invitee: sent when a peer is invited to the channel.
     *  The channel key is sent in plaintext because the PeerGraphQL transport
     *  already encrypts the entire request with the peer's shared ChaCha20 key
     *  and verifies the Ed25519 signature.
     *
     *  The invitee MUST verify [signature] using the owner's public key (taken
     *  from [memberPeers] entry whose id equals [owner]) and the payload
     *  `"$channelId|$version|invite|<own peer id>"`. The target check ensures
     *  the invite was actually addressed to this device.
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
         *  peer records for members it doesn't already have. The owner's
         *  publicKey is taken from the entry whose id matches [owner]. */
        val memberPeers: List<MemberPeerInfo> = emptyList(),
        val version: Long,
        /** Ed25519 signature of `"$channelId|$version|invite|<invitee peer id>"`
         *  (Base64), signed by the owner at send time. */
        val signature: String = "",
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
        /** Ed25519 signature of `"$channelId|$version|update|"` (Base64),
         *  signed by the owner at send time. */
        val signature: String = "",
    )

    /** Owner → Kicked peer: you have been removed from the channel. */
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

/**
 * Build the canonical payload that the owner signs for every channel system
 * message: `"$channelId|$version|$action|$target"`.
 *
 * Protocol constant — bumping this format is a breaking change, all peers
 * must agree on the exact byte sequence.
 *
 * @param target invitee/kicked peer id for targeted actions; empty string for
 *               broadcasts (e.g. update, broadcast-kick on channel delete).
 */
fun channelMessagePayload(
    channelId: String,
    version: Long,
    action: String,
    target: String,
): String = "$channelId|$version|$action|$target"