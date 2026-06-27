package com.ismartcoding.plain.chat.peer

import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.chat.ChatCacher
import com.ismartcoding.plain.chat.ChatDbHelper
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.enums.DeviceType
import com.ismartcoding.plain.helpers.TimeHelper

object PeerManager {
    suspend fun deletePeer(peerId: String): Boolean = withIO {
        val peerDao = AppDatabase.instance.peerDao()
        val peer = peerDao.getById(peerId) ?: return@withIO false

        ChatDbHelper.deleteAllChatsAsync(peerId)
        val isChannelMember = AppDatabase.instance.chatChannelDao().getAll().any { it.hasMember(peerId) }
        if (isChannelMember) {
            peer.key = ""
            peer.status = "channel"
            peerDao.update(peer)
        } else {
            peerDao.delete(peerId)
        }
        PeerCacher.removePeer(peerId)
        PeerCacher.load()
        ChatCacher.load()
        true
    }

    suspend fun markUnpaired(peerId: String): Boolean = withIO {
        val peerDao = AppDatabase.instance.peerDao()
        val peer = peerDao.getById(peerId) ?: return@withIO false
        peer.status = "unpaired"
        peer.updatedAt = TimeHelper.now()
        peerDao.update(peer)
        PeerCacher.removePeer(peerId)
        PeerCacher.load()
        LogCat.d("Device unpaired: $peerId")
        true
    }

    suspend fun applyDeviceDiscovered(
        deviceId: String,
        ips: List<String>,
        port: Int,
        name: String,
        deviceType: DeviceType,
    ): DPeer? = withIO {
        val peerDao = AppDatabase.instance.peerDao()
        val peer = peerDao.getById(deviceId) ?: return@withIO null
        if (peer.status != "paired") return@withIO null

        val newIpString = ips.joinToString(",")
        var changed = false
        if (peer.ip != newIpString) {
            peer.ip = newIpString
            changed = true
        }
        if (peer.port != port) {
            peer.port = port
            changed = true
        }
        if (peer.name != name) {
            peer.name = name
            changed = true
        }
        if (peer.deviceType != deviceType.value) {
            peer.deviceType = deviceType.value
            changed = true
        }
        if (!changed) return@withIO null

        peer.updatedAt = TimeHelper.now()
        peerDao.update(peer)

        PeerCacher.updatePeer(peer)
        peer
    }

    suspend fun upsertPaired(
        deviceId: String,
        deviceName: String,
        deviceIps: List<String>,
        port: Int,
        deviceType: DeviceType,
        key: String,
        signaturePublicKey: String,
    ) = withIO {
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
        PeerCacher.load()
        LogCat.d("Upserted peer: $deviceId")
    }

    fun setOnlineStatus(peerId: String, online: Boolean) {
        PeerCacher.setOnline(peerId, online)
    }

    suspend fun load() = withIO {
        PeerCacher.load()
        PeerCacher.setOnlineMap(
            PeerCacher.peersMap.value.values
                .filter { it.peer.isPaired() }
                .associate { it.peer.id to PeerStatusManager.isOnline(it.peer.id) }
        )
    }
}
