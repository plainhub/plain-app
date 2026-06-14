package com.ismartcoding.plain.chat.peer

import android.content.Context
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.chat.ChatCacheManager
import com.ismartcoding.plain.chat.ChatDbHelper
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.enums.DeviceType
import com.ismartcoding.plain.helpers.TimeHelper

object PeerManager {
    suspend fun deletePeer(context: Context, peerId: String): Boolean {
        val peerDao = AppDatabase.instance.peerDao()
        if (peerDao.getById(peerId) == null) return false

        ChatDbHelper.deleteAllChatsAsync(context, peerId)
        val isChannelMember = AppDatabase.instance.chatChannelDao().getAll().any { it.hasMember(peerId) }
        if (isChannelMember) {
            val peer = peerDao.getById(peerId)!!
            peer.key = ""
            peer.status = "channel"
            peerDao.update(peer)
        } else {
            peerDao.delete(peerId)
        }
        ChatCacheManager.loadKeyCacheAsync()
        return true
    }

    /**
     * Mark an existing peer as unpaired. Caller is responsible for re-loading
     * any view-model state that mirrors the peer list. Returns true when the
     * peer existed and was updated.
     */
    suspend fun markUnpaired(peerId: String): Boolean {
        val peerDao = AppDatabase.instance.peerDao()
        val peer = peerDao.getById(peerId) ?: run {
            LogCat.w("Peer not found for unpair: $peerId")
            return false
        }
        peer.status = "unpaired"
        peer.updatedAt = TimeHelper.now()
        peerDao.update(peer)
        ChatCacheManager.loadKeyCacheAsync()
        LogCat.d("Device unpaired: $peerId")
        return true
    }

    /**
     * Apply discovery metadata (IP, port, name, device type) to an already-paired
     * peer. Returns the freshly-persisted peer when at least one field changed;
     * null when the peer is missing, not paired, or unchanged.
     */
    suspend fun applyDeviceDiscovered(
        deviceId: String,
        ips: List<String>,
        port: Int,
        name: String,
        deviceType: DeviceType,
    ): DPeer? {
        val peerDao = AppDatabase.instance.peerDao()
        val peer = peerDao.getById(deviceId) ?: return null
        if (peer.status != "paired") return null

        val newIpString = ips.joinToString(",")
        var changed = false
        if (peer.ip != newIpString) { peer.ip = newIpString; changed = true }
        if (peer.port != port) { peer.port = port; changed = true }
        if (peer.name != name) { peer.name = name; changed = true }
        if (peer.deviceType != deviceType.value) { peer.deviceType = deviceType.value; changed = true }
        if (!changed) return null

        peer.updatedAt = TimeHelper.now()
        peerDao.update(peer)
        return peer
    }

    /**
     * Upsert a freshly-paired peer with the negotiated key + signature public
     * key. Preserves `createdAt` when the peer already exists.
     */
    suspend fun upsertPaired(
        deviceId: String,
        deviceName: String,
        deviceIps: List<String>,
        port: Int,
        deviceType: DeviceType,
        key: String,
        signaturePublicKey: String,
    ) {
        val now = TimeHelper.now()
        val ipString = deviceIps.joinToString(",")
        val peer = (AppDatabase.instance.peerDao().getById(deviceId) ?: DPeer(deviceId).apply {
            createdAt = now
        }).apply {
            name = deviceName
            ip = ipString
            this.port = port
            this.deviceType = deviceType.value
            this.key = key
            publicKey = signaturePublicKey
            status = "paired"
            updatedAt = now
        }
        AppDatabase.instance.peerDao().upsert(peer)
        ChatCacheManager.loadKeyCacheAsync()
        LogCat.d("Upserted peer: $deviceId")
    }
}
