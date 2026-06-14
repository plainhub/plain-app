package com.ismartcoding.plain.db

import com.ismartcoding.lib.helpers.NetworkHelper
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.enums.DeviceType
import com.ismartcoding.plain.helpers.SignatureHelper

suspend fun DChatChannel.getPeersAsync(): List<DPeer> {
    val ids = memberIds()
    val dbPeers = AppDatabase.instance.peerDao().getByIds(ids).associateBy { it.id }
    return ids.mapNotNull { peerId ->
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
