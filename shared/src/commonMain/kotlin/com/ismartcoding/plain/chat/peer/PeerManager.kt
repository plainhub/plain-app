package com.ismartcoding.plain.chat.peer

import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.chat.ChatCacher
import com.ismartcoding.plain.chat.ChatDbHelper
import com.ismartcoding.plain.platform.AppDatabase
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
    ): DPeer? {
        val existing = PeerCacher.getPeer(deviceId) ?: return null
        if (existing.status != "paired") return null

        val newIpString = ips.joinToString(",")
        val newDeviceType = deviceType.value
        return PeerCacher.mutatePeer(deviceId) { p ->
            if (p.ip != newIpString) p.ip = newIpString
            if (p.port != port) p.port = port
            if (p.name != name) p.name = name
            if (p.deviceType != newDeviceType) p.deviceType = newDeviceType
            // Always refresh updatedAt to signal we heard from this peer.
            // PeerStatusManager.reconnectPeer compares updatedAt before and after
            // a directed DISCOVER to detect whether the reply arrived within the
            // wait window. Without this, a stale IP in the DB would be reused
            // forever (observed: 370+ failed reconnect attempts to a dead IP).
            p.updatedAt = TimeHelper.now()
        }
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
