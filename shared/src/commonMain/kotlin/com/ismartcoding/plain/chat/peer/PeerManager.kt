package com.ismartcoding.plain.chat.peer


import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.chat.ChatCacher
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.enums.DeviceType

object PeerManager {
    suspend fun deletePeer(peerId: String): Boolean = withIO {
        if (!RustPeerStore.remove(peerId)) return@withIO false
        PeerCacher.removePeer(peerId)
        PeerCacher.load()
        ChatCacher.load()
        true
    }

    suspend fun markUnpaired(peerId: String): Boolean = withIO {
        if (!RustPeerStore.unpair(peerId)) return@withIO false
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
        val peer = RustPeerStore.discovered(deviceId, ips, port, name, deviceType) ?: return null
        PeerCacher.load()
        return peer
    }

    fun setOnlineStatus(peerId: String, online: Boolean) {
        PeerCacher.setOnline(peerId, online)
    }

    suspend fun load() = withIO {
        PeerCacher.load()
        PeerCacher.setOnlineMap(
            PeerCacher.peersMap.value.values
                .filter { it.isPaired() }
                .associate { it.id to PeerStatusManager.isOnline(it.id) }
        )
    }
}
