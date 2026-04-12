package com.ismartcoding.plain.features.dlna.sender

import android.content.Context
import android.net.wifi.WifiManager
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.features.dlna.common.DlnaDevice
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket
import java.util.Locale

object DlnaDeviceScanner {
    private val devices = HashSet<DlnaDevice>()
    private val searchQuery =
        "M-SEARCH * HTTP/1.1\r\nST: ssdp:all\r\nHOST: 239.255.255.250:1900\r\nMX: 3\r\nMAN: \"ssdp:discover\"\r\n\r\n"

    private const val SSDP_ADDR = "239.255.255.250"
    private const val SSDP_PORT = 1900

    fun search(context: Context): Flow<DlnaDevice> = callbackFlow {
        devices.forEach { trySend(it) }
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val lock = wifi.createMulticastLock("DlnaDeviceScanner")
        lock.acquire()
        var socket: MulticastSocket? = null
        val group = InetAddress.getByName(SSDP_ADDR)
        try {
            socket = MulticastSocket()
            socket.reuseAddress = true
            socket.joinGroup(group)
            socket.setReceiveBufferSize(32768)
            socket.broadcast = true
            socket.send(DatagramPacket(searchQuery.toByteArray(), searchQuery.length, group, SSDP_PORT))
            socket.soTimeout = 5_000
            while (isActive) {
                val packet = DatagramPacket(ByteArray(1024), 1024)
                socket.receive(packet)
                val response = String(packet.data, 0, packet.length)
                val prefix = response.take(20).uppercase(Locale.getDefault())
                if (prefix.startsWith("HTTP/1.1 200") || prefix.startsWith("NOTIFY * HTTP")) {
                    val device = DlnaDevice(packet.address.hostAddress!!, response)
                    if (devices.none { it.hostAddress == device.hostAddress }) {
                        LogCat.d(response)
                        devices.add(device)
                        trySend(device)
                    }
                }
            }
        } catch (e: Exception) {
            LogCat.e(e)
        }
        awaitClose {
            try { socket?.leaveGroup(group); socket?.close() } catch (e: Exception) { LogCat.e(e) }
            lock.release()
        }
    }
}
