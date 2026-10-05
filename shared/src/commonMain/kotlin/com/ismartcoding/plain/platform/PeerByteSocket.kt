package com.ismartcoding.plain.platform

import com.ismartcoding.plain.db.DPeer

interface PeerByteSocket {
    suspend fun read(length: Int): ByteArray?
    suspend fun write(bytes: ByteArray)
    fun close()
}

expect suspend fun openAwareByteSocket(peer: DPeer): PeerByteSocket
