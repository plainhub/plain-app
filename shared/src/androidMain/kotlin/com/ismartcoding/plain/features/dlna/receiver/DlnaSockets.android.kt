package com.ismartcoding.plain.features.dlna.receiver

import android.content.Context
import android.net.wifi.WifiManager
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.lib.logcat.LogCat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.MulticastSocket
import java.net.ServerSocket
import java.net.Socket

/**
 * Android actual implementation of [DlnaServerSocket] wrapping `java.net.ServerSocket`.
 *
 * Only raw socket I/O lives here — all HTTP parsing, routing, and SOAP handling
 * is done in commonMain via [DlnaHttpRouter].
 */
private class AndroidDlnaServerSocket(private val serverSocket: ServerSocket) : DlnaServerSocket {
    override val localPort: Int
        get() = serverSocket.localPort

    override suspend fun accept(): DlnaClientConnection? = withContext(Dispatchers.IO) {
        try {
            val client = serverSocket.accept()
            AndroidDlnaClientConnection(client)
        } catch (e: Exception) {
            LogCat.e("DLNA accept error: ${e.message}")
            null
        }
    }

    override fun close() {
        try {
            serverSocket.close()
        } catch (_: Exception) {
        }
    }
}

/**
 * Android actual implementation of [DlnaClientConnection] wrapping `java.net.Socket`.
 * Reads one HTTP request (line + headers + body) and writes the response string.
 */
private class AndroidDlnaClientConnection(private val socket: Socket) : DlnaClientConnection {
    override val senderIp: String
        get() = socket.inetAddress?.hostAddress.orEmpty()

    private val bis: BufferedInputStream = BufferedInputStream(socket.inputStream)
    private val writer: PrintWriter = PrintWriter(OutputStreamWriter(socket.outputStream, Charsets.UTF_8), false)

    init {
        socket.soTimeout = 5_000
    }

    override fun readHttpRequest(): DlnaHttpRequest? {
        try {
            val requestLine = bis.readHttpLine() ?: return null
            val parts = requestLine.split(" ")
            if (parts.size < 2) return null
            val method = parts[0]
            val path = parts[1]

            val headers = mutableMapOf<String, String>()
            var headerLine = bis.readHttpLine()
            while (!headerLine.isNullOrEmpty()) {
                val idx = headerLine.indexOf(':')
                if (idx > 0) {
                    headers[headerLine.substring(0, idx).trim().lowercase()] =
                        headerLine.substring(idx + 1).trim()
                }
                headerLine = bis.readHttpLine()
            }
            val contentLength = headers["content-length"]?.toIntOrNull() ?: 0
            val body = readBodyBytes(bis, contentLength)
            return DlnaHttpRequest(method, path, headers, body)
        } catch (e: Exception) {
            LogCat.e("DLNA read request error: ${e.message}")
            return null
        }
    }

    override fun writeResponse(response: String) {
        try {
            writer.print(response)
            writer.flush()
        } catch (e: Exception) {
            LogCat.e("DLNA write response error: ${e.message}")
        }
    }

    override fun close() {
        try {
            socket.close()
        } catch (_: Exception) {
        }
    }
}

/**
 * Android actual implementation of [DlnaSsdpSocket] wrapping `java.net.MulticastSocket`
 * with a `WifiManager.MulticastLock` so the device actually receives multicast packets.
 */
private class AndroidDlnaSsdpSocket(
    private val socket: MulticastSocket,
    private val group: InetAddress,
    private val multicastLock: WifiManager.MulticastLock?,
) : DlnaSsdpSocket {

    override suspend fun receive(timeoutMs: Int): String? = withContext(Dispatchers.IO) {
        try {
            socket.soTimeout = timeoutMs
            val buf = ByteArray(4096)
            val packet = DatagramPacket(buf, buf.size)
            socket.receive(packet)
            String(packet.data, 0, packet.length)
        } catch (_: java.net.SocketTimeoutException) {
            null
        } catch (e: Exception) {
            LogCat.e("SSDP receive error: ${e.message}")
            null
        }
    }

    override fun sendMulticast(message: String) {
        sendTo(message, group, DlnaSsdpMessages.SSDP_PORT)
    }

    override fun sendUnicast(message: String, address: String, port: Int) {
        try {
            sendTo(message, InetAddress.getByName(address), port)
        } catch (e: Exception) {
            LogCat.e("SSDP unicast send error: ${e.message}")
        }
    }

    private fun sendTo(message: String, addr: InetAddress, port: Int) {
        try {
            val bytes = message.toByteArray()
            socket.send(DatagramPacket(bytes, bytes.size, addr, port))
        } catch (e: Exception) {
            LogCat.e("SSDP send error: ${e.message}")
        }
    }

    override fun close() {
        try {
            socket.leaveGroup(group)
        } catch (_: Exception) {
        }
        try {
            socket.close()
        } catch (_: Exception) {
        }
        try {
            multicastLock?.let { if (it.isHeld) it.release() }
        } catch (_: Exception) {
        }
    }
}

/** Actual factory: opens a [ServerSocket] with SO_REUSEADDR enabled. */
actual fun createDlnaServerSocket(port: Int): DlnaServerSocket? {
    return try {
        val ss = ServerSocket()
        ss.reuseAddress = true
        ss.bind(InetSocketAddress(port))
        AndroidDlnaServerSocket(ss)
    } catch (_: Exception) {
        null
    }
}

/** Actual factory: creates a [MulticastSocket] joined to the SSDP group, with multicast lock. */
actual fun createDlnaSsdpSocket(): DlnaSsdpSocket? {
    return try {
        val wifiMgr = appContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val lock = wifiMgr.createMulticastLock("DlnaRendererSsdp").apply { acquire() }
        val group = InetAddress.getByName(DlnaSsdpMessages.SSDP_ADDR)
        val socket = MulticastSocket(null)
        socket.reuseAddress = true
        socket.bind(InetSocketAddress(DlnaSsdpMessages.SSDP_PORT))
        socket.joinGroup(group)
        AndroidDlnaSsdpSocket(socket, group, lock)
    } catch (e: Exception) {
        LogCat.e("createDlnaSsdpSocket failed: ${e.message}")
        null
    }
}

/** Reads one HTTP header line (terminated by CRLF) as ASCII from the buffered stream. */
private fun BufferedInputStream.readHttpLine(): String? {
    val sb = StringBuilder()
    var prev = -1
    while (true) {
        val b = read()
        if (b == -1) return if (sb.isEmpty()) null else sb.toString()
        if (prev == '\r'.code && b == '\n'.code) {
            sb.deleteCharAt(sb.length - 1) // remove the trailing \r
            return sb.toString()
        }
        sb.append(b.toChar())
        prev = b
    }
}

/** Reads exactly [contentLength] bytes from the stream and decodes as UTF-8. */
private fun readBodyBytes(bis: BufferedInputStream, contentLength: Int): String {
    if (contentLength <= 0) return ""
    val buf = ByteArray(contentLength)
    var offset = 0
    while (offset < contentLength) {
        val read = bis.read(buf, offset, contentLength - offset)
        if (read == -1) break
        offset += read
    }
    return String(buf, 0, offset, Charsets.UTF_8)
}
