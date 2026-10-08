package com.ismartcoding.plain.db

import androidx.room3.ColumnInfo
import com.ismartcoding.plain.lib.TimeHelper
import kotlin.time.Instant
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.ismartcoding.plain.enums.ChatChannelStatus
import com.ismartcoding.plain.enums.ChannelMemberStatus
import com.ismartcoding.plain.lib.generateId
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

/** A channel member: peer id + membership status.
 *  All other peer metadata (name, publicKey, IP, port, etc.) is stored in the `peers` table.
 *  `@JsonNames("id")` keeps the pre-rename wire/DB JSON key `id` decodable —
 *  invites/updates from older app versions and legacy rows still parse. */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ChannelMember(
    @JsonNames("id") val peerId: String,
    /** JOINED or PENDING */
    val status: ChannelMemberStatus = ChannelMemberStatus.JOINED,
) {
    fun isJoined(): Boolean = status == ChannelMemberStatus.JOINED
    fun isPending(): Boolean = status == ChannelMemberStatus.PENDING
}

@Entity(tableName = "chat_channels")
data class DChatChannel(
    @PrimaryKey var id: String = generateId(),
    @ColumnInfo(name = "name") var name: String = "",
    @ColumnInfo(name = "key") var key: String = "",
    /** peer.id of the device that created this channel.
     *  Sentinel value "me" when this device is the owner. */
    @ColumnInfo(name = "owner_id", defaultValue = "") var ownerId: String = "",
    /** All channel members (both joined and pending).
     *  Each entry carries only the peer id and membership status;
     *  other metadata (name, publicKey, IP, port) lives in the `peers` table. */
    @ColumnInfo(name = "members") var members: List<ChannelMember> = emptyList(),
    /** Monotonically increasing counter; incremented on every mutation.
     *  Receivers ignore updates whose version ≤ their local version. */
    @ColumnInfo(name = "version", defaultValue = "0") var version: Long = 0,
    @ColumnInfo(name = "status", defaultValue = "JOINED") var status: ChatChannelStatus = ChatChannelStatus.JOINED,

    @ColumnInfo(name = "created_at") var createdAt: Instant = TimeHelper.now(),
    @ColumnInfo(name = "updated_at") var updatedAt: Instant = TimeHelper.now(),
) {

    // ── Helpers ─────────────────────────────────────────────────────

    fun memberIds(): List<String> = members.map { it.peerId }
    fun memberIdsNotMe(myId: String): List<String> = members.filter { it.peerId != myId }.map { it.peerId }

    fun joinedMembers(): List<ChannelMember> = members.filter { it.isJoined() }

    fun pendingMembers(): List<ChannelMember> = members.filter { it.isPending() }

    fun hasMember(peerId: String): Boolean = members.any { it.peerId == peerId }

    fun findMember(peerId: String): ChannelMember? = members.find { it.peerId == peerId }

    fun isJoined(): Boolean = status == ChatChannelStatus.JOINED
}
