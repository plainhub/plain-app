package com.ismartcoding.plain.discover

import com.ismartcoding.plain.chat.peer.PeerManager
import com.ismartcoding.plain.platform.AppDatabase
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.enums.DeviceType
import com.ismartcoding.plain.helpers.TimeHelper
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.lib.logcat.LogCat

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
            val now = TimeHelper.now()
            val peer = (AppDatabase.instance.peerDao().getById(deviceId) ?: DPeer(deviceId).apply {
                createdAt = now
            }).apply {
                name = deviceName
                this.ip = deviceIps.joinToString(",")
                this.port = port
                this.deviceType = deviceType.value
                this.key = key
                publicKey = signaturePublicKey
                status = "paired"
                updatedAt = now
            }
            // Peers are identified by their clientId (the deviceId). Only an
            // 8-byte shortId hash of the clientId is broadcast in the BLE scan
            // response serviceData — no BLE MAC is stored. awareSupported is
            // also not persisted; the in-memory PeerCacher.awareSupportedMap
            // (refreshed from BLE scan flags + GATT DISCOVER reply) is the
            // source of truth.
            AppDatabase.instance.peerDao().upsert(peer)
            PeerManager.load()
            LogCat.d("Upserted peer: $deviceId")
        } catch (e: Exception) {
            LogCat.e("Error storing peer in database: ${e.message}")
            throw e
        }
    }
}
