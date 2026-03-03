package com.ismartcoding.plain.chat

import android.util.Base64
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DPeer

object ChatCacheManager {
    val peerKeyCache = mutableMapOf<String, ByteArray>()
    val peerPublicKeyCache = mutableMapOf<String, ByteArray>()
    val channelKeyCache = mutableMapOf<String, ByteArray>()
    /** Cache of peer id → DPeer used by chat UI to resolve sender info (name, avatar, etc.). */
    val peerMap = mutableMapOf<String, DPeer>()

    // The peer ID whose ChatPage is currently open; empty when no peer chat is active.
    var activeChatPeerId = ""

    // The channel ID whose ChatPage is currently open; empty when no channel chat is active.
    var activeChatChannelId = ""

    suspend fun loadKeyCacheAsync() {
        peerKeyCache.clear()
        peerPublicKeyCache.clear()
        channelKeyCache.clear()

        // Load keys from peers table (paired AND channel peers).
        // Channel-status peers have key="" but carry a publicKey for signature verification.
        val peers = AppDatabase.instance.peerDao().getAllWithPublicKey()
        peers.forEach { peer ->
            if (peer.key.isNotEmpty()) {
                peerKeyCache[peer.id] = Base64.decode(peer.key, Base64.NO_WRAP)
            }
            if (peer.publicKey.isNotEmpty()) {
                peerPublicKeyCache[peer.id] = Base64.decode(peer.publicKey, Base64.NO_WRAP)
            }
        }

        // Load keys from chat_channels table (only active channels)
        val channels = AppDatabase.instance.chatChannelDao().getAll()
        channels.forEach { channel ->
            // Exclude left/kicked channels so we reject incoming messages
            if (channel.status == "joined") {
                channelKeyCache[channel.id] = Base64.decode(channel.key, Base64.NO_WRAP)
            }
        }
    }

    fun getKey(type: String, id: String): ByteArray? {
        return when (type) {
            "peer" -> peerKeyCache[id]
            "channel" -> channelKeyCache[id]
            else -> null
        }
    }

    /** Rebuild [peerMap] from the latest [peers] data. */
    fun refreshPeerMap(peers: List<DPeer>) {
        val cache = mutableMapOf<String, DPeer>()
        peers.forEach { peer ->
            cache[peer.id] = peer
        }
        peerMap.clear()
        peerMap.putAll(cache)
    }
}