package com.ismartcoding.plain.features.dlna.receiver

import android.content.Context
import android.net.wifi.WifiManager
import com.ismartcoding.lib.helpers.NetworkHelper
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.features.dlna.DlnaRendererState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket

/** Advertises this device as a UPnP MediaRenderer via SSDP multicast. */
object DlnaSsdpAdvertiser {

    private const val SSDP_ADDR = "239.255.255.250"
    private const val SSDP_PORT = 1900
    private const val DEVICE_TYPE = "urn:schemas-upnp-org:device:MediaRenderer:1"
    private const val AVT_TYPE = "urn:schemas-upnp-org:service:AVTransport:1"

    suspend fun run(context: Context) = withContext(Dispatchers.IO) {
        val wifiMgr = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val lock = wifiMgr.createMulticastLock("DlnaRendererSsdp")
        lock.acquire()
        val group = InetAddress.getByName(SSDP_ADDR)
        var socket: MulticastSocket? = null
        try {
            socket = MulticastSocket(null)
            socket.reuseAddress = true
            socket.bind(java.net.InetSocketAddress(SSDP_PORT))
            socket.joinGroup(group)
            sendAlive(socket, group)
            val buf = ByteArray(4096)
            val packet = DatagramPacket(buf, buf.size)
            while (isActive) {
                socket.soTimeout = 30_000
                try {
                    socket.receive(packet)
                    val msg = String(packet.data, 0, packet.length)
                    if (msg.contains("M-SEARCH")) {
                        respondMSearch(socket, packet.address, packet.port)
                    }
                } catch (_: java.net.SocketTimeoutException) {
                    sendAlive(socket, group)
                }
            }
        } catch (e: Exception) {
            LogCat.e("DLNA SSDP error: ${e.message}")
        } finally {
            try { sendByeBye(socket, group) } catch (_: Exception) {}
            socket?.close()
            lock.release()
        }
    }

    private fun sendAlive(socket: MulticastSocket, group: InetAddress) {
        val uuid = "uuid:${DlnaRenderer.deviceUuid}"
        listOf(
            notifyMsg(uuid, "upnp:rootdevice", "ssdp:alive"),
            notifyMsg("$uuid::$DEVICE_TYPE", DEVICE_TYPE, "ssdp:alive"),
            notifyMsg("$uuid::$AVT_TYPE", AVT_TYPE, "ssdp:alive"),
        ).forEach { send(socket, it, group, SSDP_PORT) }
    }

    private fun sendByeBye(socket: MulticastSocket?, group: InetAddress) {
        socket ?: return
        val uuid = "uuid:${DlnaRenderer.deviceUuid}"
        listOf(
            notifyMsg(uuid, "upnp:rootdevice", "ssdp:byebye"),
            notifyMsg("$uuid::$DEVICE_TYPE", DEVICE_TYPE, "ssdp:byebye"),
            notifyMsg("$uuid::$AVT_TYPE", AVT_TYPE, "ssdp:byebye"),
        ).forEach { send(socket, it, group, SSDP_PORT) }
    }

    private fun respondMSearch(socket: MulticastSocket, addr: InetAddress, port: Int) {
        val uuid = "uuid:${DlnaRenderer.deviceUuid}"
        listOf(
            searchResponse("upnp:rootdevice", "$uuid::upnp:rootdevice"),
            searchResponse(DEVICE_TYPE, "$uuid::$DEVICE_TYPE"),
            searchResponse(AVT_TYPE, "$uuid::$AVT_TYPE"),
        ).forEach { send(socket, it, addr, port) }
    }

    private fun notifyMsg(usn: String, nt: String, nts: String): String {
        val ip = NetworkHelper.getDeviceIP4()
        val port = DlnaRendererState.port.value
        return "NOTIFY * HTTP/1.1\r\nHOST: $SSDP_ADDR:$SSDP_PORT\r\n" +
            "CACHE-CONTROL: max-age=1800\r\nLOCATION: http://$ip:$port/description.xml\r\n" +
            "NT: $nt\r\nNTS: $nts\r\nSERVER: Android/1.0 UPnP/1.1 PlainApp/1.0\r\nUSN: $usn\r\n\r\n"
    }

    private fun searchResponse(st: String, usn: String): String {
        val ip = NetworkHelper.getDeviceIP4()
        val port = DlnaRendererState.port.value
        return "HTTP/1.1 200 OK\r\nCACHE-CONTROL: max-age=1800\r\n" +
            "LOCATION: http://$ip:$port/description.xml\r\n" +
            "SERVER: Android/1.0 UPnP/1.1 PlainApp/1.0\r\nST: $st\r\nUSN: $usn\r\n\r\n"
    }

    private fun send(socket: MulticastSocket, message: String, addr: InetAddress, port: Int) {
        try {
            val bytes = message.toByteArray()
            socket.send(DatagramPacket(bytes, bytes.size, addr, port))
        } catch (e: Exception) {
            LogCat.e("SSDP send error: ${e.message}")
        }
    }
}
