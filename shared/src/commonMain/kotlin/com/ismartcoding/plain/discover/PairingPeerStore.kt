package com.ismartcoding.plain.discover

import com.ismartcoding.plain.chat.peer.PeerManager
import com.ismartcoding.plain.enums.DeviceType

object PairingPeerStore {
    suspend fun save(deviceId: String, deviceName: String, deviceIps: List<String>, port: Int, deviceType: DeviceType, key: String, signaturePublicKey: String) {
        RustPairingStore.save(deviceId, deviceName, deviceIps, port, deviceType, key, signaturePublicKey)
        PeerManager.load()
    }
}
