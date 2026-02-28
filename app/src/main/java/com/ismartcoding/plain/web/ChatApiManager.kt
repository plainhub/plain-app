package com.ismartcoding.plain.web

import android.util.Base64
import com.ismartcoding.plain.db.AppDatabase

object ChatApiManager {
    val peerKeyCache = mutableMapOf<String, ByteArray>()
    val peerPublicKeyCache = mutableMapOf<String, ByteArray>()
    val channelKeyCache = mutableMapOf<String, ByteArray>()
    val clientRequestTs = mutableMapOf<String, Long>()

    suspend fun loadKeyCacheAsync() {
        peerKeyCache.clear()
        peerPublicKeyCache.clear()
        channelKeyCache.clear()

        // Load keys from peers table (only paired peers)
        val peers = AppDatabase.Companion.instance.peerDao().getAllPaired()
        peers.forEach { peer ->
            peerKeyCache[peer.id] = Base64.decode(peer.key, Base64.NO_WRAP)
            peerPublicKeyCache[peer.id] = Base64.decode(peer.publicKey, Base64.NO_WRAP)
        }

        // Load keys from chat_channels table
        val channels = AppDatabase.Companion.instance.chatChannelDao().getAll()
        channels.forEach { channel ->
            channelKeyCache[channel.id] = Base64.decode(channel.key, Base64.NO_WRAP)
        }
    }

    fun getKey(type: String, id: String): ByteArray? {
        return when (type) {
            "peer" -> peerKeyCache[id]
            "channel" -> channelKeyCache[id]
            else -> null
        }
    }
}