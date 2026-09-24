package com.ismartcoding.plain.db

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import com.ismartcoding.plain.lib.TimeHelper
import kotlin.time.Instant
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.Update
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

    /**
     * Elect a leader for this channel from the joined members.
     *
     * Rules (in priority order):
     * 1. The owner is preferred if online.
     * 2. Otherwise, the online joined member with the smallest id.
     *
     * @param onlinePeerIds set of peer ids known to be online right now.
     *        The local device's own id is always considered online.
     * @param myId the local device's peer id.
     * @return the peer id of the elected leader, or null if no eligible member is online.
     */
    fun electLeader(onlinePeerIds: Set<String>, myId: String): String? {
        val joined = joinedMembers()
        val onlineJoined = joined.filter { it.peerId == myId || onlinePeerIds.contains(it.peerId) }
        if (onlineJoined.isEmpty()) return null

        // Resolve the owner's real peer id ("me" sentinel → myId)
        val ownerPeerId = if (ownerId == "me") myId else ownerId
        if (onlineJoined.any { it.peerId == ownerPeerId }) return ownerPeerId

        // Fallback: smallest id among online joined members
        return onlineJoined.minByOrNull { it.peerId }?.peerId
    }

    /** Check whether this device is currently the channel leader. */
    fun isLeader(onlinePeerIds: Set<String>, myId: String): Boolean {
        return electLeader(onlinePeerIds, myId) == myId
    }
}

@Dao
interface ChatChannelDao {
    @Query("SELECT * FROM chat_channels")
    suspend fun getAll(): List<DChatChannel>

    @Query("SELECT * FROM chat_channels WHERE id = :id")
    suspend fun getById(id: String): DChatChannel?

    @Query("SELECT * FROM chat_channels WHERE owner_id = 'me'")
    suspend fun getOwnedChannels(): List<DChatChannel>

    @Insert
    suspend fun insert(vararg item: DChatChannel)

    @Update
    suspend fun update(vararg item: DChatChannel)

    @Query("DELETE FROM chat_channels WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM chat_channels WHERE id in (:ids)")
    suspend fun deleteByIds(ids: List<String>)
}
