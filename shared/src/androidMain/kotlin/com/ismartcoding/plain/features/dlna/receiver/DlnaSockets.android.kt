package com.ismartcoding.plain.features.dlna.receiver

import android.content.Context
import android.net.wifi.WifiManager
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.lib.dlna.common.DlnaSsdpMessages
import com.ismartcoding.plain.lib.dlna.common.DlnaSsdpPacket
import com.ismartcoding.plain.lib.dlna.common.DlnaSsdpSocket
import com.ismartcoding.plain.lib.logcat.LogCat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.MulticastSocket

/**
 * Android actual implementation of [DlnaSsdpSocket] wrapping `java.net.MulticastSocket`
 * with a `WifiManager.MulticastLock` so the device actually receives multicast packets.
 */
private class AndroidDlnaSsdpSocket(
    private val socket: MulticastSocket,
    private val group: InetAddress,
    private val multicastLock: WifiManager.MulticastLock?,
) : DlnaSsdpSocket {

    override suspend fun receive(timeoutMs: Int): DlnaSsdpPacket? = withContext(Dispatchers.IO) {
        try {
            socket.soTimeout = timeoutMs
            val buf = ByteArray(4096)
            val packet = DatagramPacket(buf, buf.size)
            socket.receive(packet)
            DlnaSsdpPacket(
                message = String(packet.data, 0, packet.length),
                sourceAddress = packet.address?.hostAddress ?: "",
                sourcePort = packet.port,
            )
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

/** Actual factory: creates a [MulticastSocket] joined to the SSDP group, with multicast lock. */
actual fun createDlnaSsdpSocket(bindPort: Int): DlnaSsdpSocket? {
    return try {
        val wifiMgr = appContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val lock = wifiMgr.createMulticastLock("DlnaRendererSsdp").apply { acquire() }
        val group = InetAddress.getByName(DlnaSsdpMessages.SSDP_ADDR)
        val socket = MulticastSocket(null)
        socket.reuseAddress = true
        // Bind explicitly to the IPv4 wildcard so the socket lives in the IPv4
        // family — a bare InetSocketAddress(port) may bind to IPv6 (::) on some
        // Android builds, which complicates IPv4 multicast (239.255.255.250)
        // join/receive and breaks unicast replies to IPv4 control points.
        socket.bind(InetSocketAddress(InetAddress.getByName("0.0.0.0"), bindPort))
        socket.joinGroup(group)
        LogCat.d("DLNA SSDP socket bound to 0.0.0.0:$bindPort joined $group")
        AndroidDlnaSsdpSocket(socket, group, lock)
    } catch (e: Exception) {
        LogCat.e("createDlnaSsdpSocket failed: ${e.message}")
        null
    }
}
