package com.ismartcoding.plain.discover

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
        bleAddress: String = "",
        awareSupported: Boolean = false,
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
            // Persist the peer's BLE address (MAC on Android, UUID on iOS) so
            // the BLE chat fallback can reconnect without scanning. Preserve a
            // previously learned address when the caller doesn't supply one —
            // LAN-initiated pairing leaves bleAddress blank but shouldn't wipe
            // an address captured during an earlier BLE pairing.
            if (bleAddress.isNotEmpty()) {
                peer.bleAddress = bleAddress
            }
            // Always update awareSupported — it reflects the peer's current
            // device capability reported in the latest pairing handshake.
            peer.awareSupported = awareSupported
            AppDatabase.instance.peerDao().upsert(peer)
            LogCat.d("Upserted peer: $deviceId ble=${peer.bleAddress} aware=${peer.awareSupported}")
        } catch (e: Exception) {
            LogCat.e("Error storing peer in database: ${e.message}")
            throw e
        }
    }
}
