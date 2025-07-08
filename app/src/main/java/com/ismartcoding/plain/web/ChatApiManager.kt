package com.ismartcoding.plain.web

import android.util.Base64
import com.ismartcoding.plain.db.AppDatabase

object ChatApiManager {
    val peerKeyCache = mutableMapOf<String, ByteArray>()
    val peerPublicKeyCache = mutableMapOf<String, ByteArray>()
    val groupKeyCache = mutableMapOf<String, ByteArray>()
    val clientRequestTs = mutableMapOf<String, Long>()

    suspend fun loadKeyCacheAsync() {
        peerKeyCache.clear()
        peerPublicKeyCache.clear()
        groupKeyCache.clear()

        // Load keys from peers table (only paired peers)
        val peers = AppDatabase.Companion.instance.peerDao().getAllPaired()
        peers.forEach { peer ->
            peerKeyCache[peer.id] = Base64.decode(peer.key, Base64.NO_WRAP)
            peerPublicKeyCache[peer.id] = Base64.decode(peer.publicKey, Base64.NO_WRAP)
        }

        // Load keys from chat_groups table
        val groups = AppDatabase.Companion.instance.chatGroupDao().getAll()
        groups.forEach { group ->
            groupKeyCache[group.id] = Base64.decode(group.key, Base64.NO_WRAP)
        }
    }

    fun getKey(type: String, id: String): ByteArray? {
        return when (type) {
            "peer" -> peerKeyCache[id]
            "group" -> groupKeyCache[id]
            else -> null
        }
    }
}