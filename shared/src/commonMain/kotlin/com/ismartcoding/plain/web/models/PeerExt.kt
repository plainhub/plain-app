package com.ismartcoding.plain.web.models

import com.ismartcoding.plain.chat.peer.PeerStatusManager
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.db.getBestIp
import com.ismartcoding.plain.enums.DeviceType

fun DPeer.toModel(): Peer {
    return Peer(id, name, getBestIp(), status, port, DeviceType.fromValue(deviceType), createdAt, updatedAt, PeerStatusManager.isOnline(id))
}
