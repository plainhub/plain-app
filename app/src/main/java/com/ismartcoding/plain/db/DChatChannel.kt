package com.ismartcoding.plain.db

import android.util.Base64
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import com.ismartcoding.lib.helpers.CryptoHelper
import com.ismartcoding.lib.helpers.NetworkHelper
import com.ismartcoding.lib.helpers.StringHelper
import com.ismartcoding.plain.R
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.enums.DeviceType
import com.ismartcoding.plain.features.locale.LocaleHelper.getString
import com.ismartcoding.plain.helpers.SignatureHelper
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
}

@Entity(tableName = "chat_channels")
data class DChatChannel(
    @PrimaryKey var id: String = StringHelper.shortUUID(),
) : DEntityBase() {
    @ColumnInfo(name = "name")
    var name: String = ""

    @ColumnInfo(name = "key")
    var key: String = ""

    /** peer.id of the device that created this channel.
     *  Sentinel value "me" when this device is the owner. */
    @ColumnInfo(name = "owner", defaultValue = "")
    var owner: String = ""

    /** All channel members (both joined and pending).
     *  Each entry carries only the peer id and membership status;
     *  other metadata (name, publicKey, IP, port) lives in the `peers` table. */
    @ColumnInfo(name = "members")
    var members: List<ChannelMember> = emptyList()

    /** Monotonically increasing counter; incremented on every mutation.
     *  Receivers ignore updates whose version ≤ their local version. */
    @ColumnInfo(name = "version", defaultValue = "0")
    var version: Long = 0

    @ColumnInfo(name = "status", defaultValue = STATUS_JOINED)
    var status: String = STATUS_JOINED

    // ── Helpers ─────────────────────────────────────────────────────

    fun memberIds(): List<String> = members.map { it.id }

    fun joinedMembers(): List<ChannelMember> = members.filter { it.isJoined() }

    fun pendingMembers(): List<ChannelMember> = members.filter { it.isPending() }

    fun hasMember(peerId: String): Boolean = members.any { it.id == peerId }

    fun findMember(peerId: String): ChannelMember? = members.find { it.id == peerId }

    suspend fun getPeersAsync(): List<DPeer> {
        val ids = memberIds()
        val dbPeers = AppDatabase.instance.peerDao().getByIds(ids).associateBy { it.id }
        return ids.map { peerId ->
            if (peerId == TempData.clientId) {
                DPeer(
                    id = peerId,
                    name = TempData.deviceName,
                    ip = NetworkHelper.getDeviceIP4s().joinToString(","),
                    port = TempData.httpsPort,
                    publicKey = SignatureHelper.getRawPublicKeyBase64Async(),
                    deviceType = DeviceType.PHONE.value,
                )
            } else {
                dbPeers[peerId] ?: DPeer(
                    id = peerId,
                    name = "",
                    deviceType = DeviceType.PHONE.value,
                )
            }
        }
    }

    /**
     * Elect a leader for this channel from the joined members.
     *
     * Rules (in priority order):
     * 1. The owner is preferred if online.
     * 2. Otherwise, the online joined member with the smallest id.
     *
     * @param onlinePeerIds set of peer ids known to be online right now.
     *        The local device's own id (`TempData.clientId`) is always considered online.
     * @return the peer id of the elected leader, or null if no eligible member is online.
     */
    fun electLeader(onlinePeerIds: Set<String>): String? {
        val myId = TempData.clientId
        val joined = joinedMembers()
        val onlineJoined = joined.filter { it.id == myId || onlinePeerIds.contains(it.id) }
        if (onlineJoined.isEmpty()) return null

        // Resolve the owner's real peer id ("me" sentinel → TempData.clientId)
        val ownerPeerId = if (owner == "me") myId else owner
        if (onlineJoined.any { it.id == ownerPeerId }) return ownerPeerId

        // Fallback: smallest id among online joined members
        return onlineJoined.minByOrNull { it.id }?.id
    }

    /** Check whether this device is currently the channel leader. */
    fun isLeader(onlinePeerIds: Set<String>): Boolean {
        return electLeader(onlinePeerIds) == TempData.clientId
    }

    companion object {
        const val STATUS_JOINED = "joined"
        const val STATUS_PENDING = "left"
    }
}

@Dao
interface ChatChannelDao {
    @Query("SELECT * FROM chat_channels")
    fun getAll(): List<DChatChannel>

    @Query("SELECT * FROM chat_channels WHERE id = :id")
    fun getById(id: String): DChatChannel?

    @Query("SELECT * FROM chat_channels WHERE owner = 'me'")
    fun getOwnedChannels(): List<DChatChannel>

    @Insert
    fun insert(vararg item: DChatChannel)

    @Update
    fun update(vararg item: DChatChannel)

    @Query("DELETE FROM chat_channels WHERE id = :id")
    fun delete(id: String)

    @Query("DELETE FROM chat_channels WHERE id in (:ids)")
    fun deleteByIds(ids: List<String>)
} 