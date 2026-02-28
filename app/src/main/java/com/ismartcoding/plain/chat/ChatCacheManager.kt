package com.ismartcoding.plain.chat

import android.util.Base64
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DPeer
import kotlin.collections.forEach

object ChatCacheManager {
    val peerKeyCache = mutableMapOf<String, ByteArray>()
    val peerPublicKeyCache = mutableMapOf<String, ByteArray>()
    val channelKeyCache = mutableMapOf<String, ByteArray>()
    /** Cache of peer id → display name used by chat UI to resolve sender names.
     *  Local peers data takes priority; channel member names fill in the rest. */
    val peerNamesCache = mutableMapOf<String, String>()

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

        // Load keys from chat_channels table
        val channels = AppDatabase.instance.chatChannelDao().getAll()
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

    /** Rebuild [peerNamesCache] from the latest [peers] data.
     *  Channel members no longer carry names; all names come from the peers table. */
    fun refreshPeerNamesCache(peers: List<DPeer>) {
        val cache = mutableMapOf<String, String>()
        peers.forEach { peer ->
            if (peer.name.isNotEmpty()) {
                cache[peer.id] = peer.name
            }
        }
        peerNamesCache.clear()
        peerNamesCache.putAll(cache)
    }
}