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
                name = TempData.deviceName,
                ip = NetworkHelper.getDeviceIP4s().joinToString(","),
                port = TempData.httpsPort,
                publicKey = SignatureHelper.getRawPublicKeyBase64Async(),
                deviceType = DeviceType.PHONE.value,
            )
        } else {
            dbPeers[peerId]
        }
    }
}
