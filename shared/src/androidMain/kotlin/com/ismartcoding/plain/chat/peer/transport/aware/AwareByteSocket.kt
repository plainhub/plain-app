package com.ismartcoding.plain.chat.peer.transport.aware

import com.ismartcoding.plain.platform.PeerByteSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException

internal class AwareByteSocket(private val socket: Socket) : PeerByteSocket {
    override suspend fun read(length: Int): ByteArray? = withContext(Dispatchers.IO) {
        val bytes = ByteArray(length)
        try { val n = socket.getInputStream().read(bytes); if (n < 0) byteArrayOf() else bytes.copyOf(n) }
        catch (_: SocketTimeoutException) { null }
    }
    override suspend fun write(bytes: ByteArray) = withContext(Dispatchers.IO) {
        socket.getOutputStream().write(bytes)
    }
    override fun close() { socket.close() }
    companion object {
        suspend fun open(connection: PeerConnection): PeerByteSocket = withContext(Dispatchers.IO) {
            val socket = connection.network.socketFactory.createSocket()
            try {
                socket.soTimeout = 1000
                socket.connect(InetSocketAddress(connection.peerIpv6, connection.peerPort), 5000)
                AwareByteSocket(socket)
            } catch (error: Throwable) { socket.close(); throw error }
        }
    }
}
