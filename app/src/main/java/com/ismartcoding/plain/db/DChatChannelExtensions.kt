package com.ismartcoding.plain.db

import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.helpers.NetworkHelper
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.chat.ChatCacheManager
import com.ismartcoding.plain.enums.DeviceType
import com.ismartcoding.plain.helpers.SignatureHelper

suspend fun DChatChannel.getPeersAsync(): List<DPeer> = withIO {
    val ids = memberIds()
    val dbPeers = AppDatabase.instance.peerDao().getByIds(ids).associateBy { it.id }
    ids.mapNotNull { peerId ->
        if (peerId == TempData.clientId) {
            DPeer(
                id = peerId,
                name = TempData.deviceName.value,
                ip = NetworkHelper.getDeviceIP4s().joinToString(","),
                port = TempData.httpsPort.value,
                publicKey = SignatureHelper.getRawPublicKeyBase64Async(),
                deviceType = DeviceType.PHONE.value,
            )
        } else {
            dbPeers[peerId]
        }
    }
}

/** True when this device owns the channel. */
fun DChatChannel.isOwnedByMe(): Boolean = owner == "me"

/** True when the channel is still actively joined on this device. */
fun DChatChannel.isJoined(): Boolean = status == DChatChannel.STATUS_JOINED

/**
 * The owning device must call `delete` rather than `leave`; everyone else
 * can call `delete` once they've left or been kicked.
 */
fun DChatChannel.canDeleteFromThisDevice(): Boolean =
    isOwnedByMe() || status == DChatChannel.STATUS_LEFT || status == DChatChannel.STATUS_KICKED

/** Only non-owners can leave. Owners should delete the channel. */
fun DChatChannel.canLeave(): Boolean = !isOwnedByMe() && isJoined()

/**
 * Build a [DPeer] representation of this device. The local device is never
 * a row in the `peers` table, so anywhere we need a peer for "me" we
 * synthesize it from in-memory state ([TempData] + [NetworkHelper]).
 */
internal fun mePeer(): DPeer = DPeer(
    id = TempData.clientId,
    name = TempData.deviceName.value,
    ip = NetworkHelper.getDeviceIP4s().joinToString(","),
    port = TempData.httpsPort.value,
    deviceType = DeviceType.PHONE.value,
)

/**
 * Resolve this channel's owner to a [DPeer]. The owner field carries the
 * "me" sentinel when this device is the owner; in that case we synthesize
 * a peer for this device via [mePeer]. For remote owners we look the peer
 * up in [ChatCacheManager.peerMap]. Returns null when the owner peer is
 * not in the cache (e.g. not yet discovered / stale).
 */
fun DChatChannel.getOwner(): DPeer? {
    val ownerId = if (owner == "me" || owner == TempData.clientId) TempData.clientId else owner
    return if (ownerId == TempData.clientId) mePeer() else ChatCacheManager.peerMap[ownerId]
}
