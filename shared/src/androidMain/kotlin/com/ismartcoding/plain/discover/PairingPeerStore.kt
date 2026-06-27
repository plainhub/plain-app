package com.ismartcoding.plain.discover

import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.chat.peer.PeerManager
import com.ismartcoding.plain.enums.DeviceType

object PairingPeerStore {
    suspend fun save(
        deviceId: String,
        deviceName: String,
        deviceIps: List<String>,
        port: Int,
        deviceType: DeviceType,
        key: String,
        signaturePublicKey: String,
    ) = withIO {
        try {
            PeerManager.upsertPaired(
                deviceId = deviceId,
                deviceName = deviceName,
                deviceIps = deviceIps,
                port = port,
                deviceType = deviceType,
                key = key,
                signaturePublicKey = signaturePublicKey,
            )
        } catch (e: Exception) {
            LogCat.e("Error storing peer in database: ${e.message}")
            throw e
        }
    }
}
