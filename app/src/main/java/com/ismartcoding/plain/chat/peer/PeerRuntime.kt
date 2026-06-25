package com.ismartcoding.plain.chat.peer

import com.ismartcoding.plain.db.DPeer

data class PeerRuntime(
    val peer: DPeer,
    val keyBytes: ByteArray,
    val publicKeyBytes: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PeerRuntime

        if (peer != other.peer) return false
        if (!keyBytes.contentEquals(other.keyBytes)) return false
        if (!publicKeyBytes.contentEquals(other.publicKeyBytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = peer.hashCode()
        result = 31 * result + keyBytes.contentHashCode()
        result = 31 * result + publicKeyBytes.contentHashCode()
        return result
    }
}