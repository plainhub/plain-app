package com.ismartcoding.plain.db

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.helpers.generateId
import kotlinx.serialization.Serializable

/** A channel member: peer id + membership status.
 *  All other peer metadata (name, publicKey, IP, port, etc.) is stored in the `peers` table. */
@Serializable
data class ChannelMember(
    val id: String,
    /** "joined" or "pending" */
    val status: String = STATUS_JOINED,
) {
    companion object {
        const val STATUS_JOINED = "joined"
        const val STATUS_PENDING = "pending"
    }

    fun isJoined(): Boolean = status == STATUS_JOINED
    fun isPending(): Boolean = status == STATUS_PENDING
    fun isMe(): Boolean = id == TempData.clientId
}

@Entity(tableName = "chat_channels")
data class DChatChannel(
    @PrimaryKey var id: String = generateId(),
    @ColumnInfo(name = "name") var name: String = "",
    @ColumnInfo(name = "key") var key: String = "",
    /** peer.id of the device that created this channel.
     *  Sentinel value "me" when this device is the owner. */
    @ColumnInfo(name = "owner", defaultValue = "") var owner: String = "",
    /** All channel members (both joined and pending).
     *  Each entry carries only the peer id and membership status;
     *  other metadata (name, publicKey, IP, port) lives in the `peers` table. */
    @ColumnInfo(name = "members") var members: List<ChannelMember> = emptyList(),
    /** Monotonically increasing counter; incremented on every mutation.
     *  Receivers ignore updates whose version ≤ their local version. */
    @ColumnInfo(name = "version", defaultValue = "0") var version: Long = 0,
    @ColumnInfo(name = "status", defaultValue = "joined") var status: String = STATUS_JOINED,
) : DEntityBase() {

    // ── Helpers ─────────────────────────────────────────────────────

    fun memberIds(): List<String> = members.map { it.id }
    fun memberIdsNotMe(myId: String): List<String> = members.filter { it.id != myId }.map { it.id }

    fun joinedMembers(): List<ChannelMember> = members.filter { it.isJoined() }

    fun pendingMembers(): List<ChannelMember> = members.filter { it.isPending() }

    fun hasMember(peerId: String): Boolean = members.any { it.id == peerId }

    fun findMember(peerId: String): ChannelMember? = members.find { it.id == peerId }

    fun isJoined(): Boolean = status == DChatChannel.STATUS_JOINED

    fun isOwnedByMe(): Boolean {
        return owner == "me" || owner == TempData.clientId
    }

    fun canLeave(): Boolean = !isOwnedByMe() && isJoined()

    fun canDeleteFromThisDevice(): Boolean =
        isOwnedByMe() || status == STATUS_LEFT || status == STATUS_KICKED

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
        val onlineJoined = joined.filter { it.id == myId || onlinePeerIds.contains(it.id) }
        if (onlineJoined.isEmpty()) return null

        // Resolve the owner's real peer id ("me" sentinel → myId)
        val ownerPeerId = if (owner == "me") myId else owner
        if (onlineJoined.any { it.id == ownerPeerId }) return ownerPeerId

        // Fallback: smallest id among online joined members
        return onlineJoined.minByOrNull { it.id }?.id
    }

    /** Check whether this device is currently the channel leader. */
    fun isLeader(onlinePeerIds: Set<String>, myId: String): Boolean {
        return electLeader(onlinePeerIds, myId) == myId
    }

    /**
     * Compute the list of peer IDs that should receive a channel message (everyone except self).
     * Only includes joined members.
     */
    fun getRecipientIds(): List<String> {
        return joinedMembers()
            .map { it.id }
            .distinct()
            .filter { it != TempData.clientId }
    }

    companion object {
        const val STATUS_JOINED = "joined"
        const val STATUS_LEFT = "left"
        const val STATUS_KICKED = "kicked"
    }
}

@Dao
interface ChatChannelDao {
    @Query("SELECT * FROM chat_channels")
    suspend fun getAll(): List<DChatChannel>

    @Query("SELECT * FROM chat_channels WHERE id = :id")
    suspend fun getById(id: String): DChatChannel?

    @Query("SELECT * FROM chat_channels WHERE owner = 'me'")
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
