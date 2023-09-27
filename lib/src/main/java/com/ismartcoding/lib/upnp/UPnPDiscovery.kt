package com.ismartcoding.lib.upnp

import android.content.Context
import android.net.wifi.WifiManager
import com.ismartcoding.lib.logcat.LogCat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket
import java.util.*

object UPnPDiscovery {
    private val devices = HashSet<UPnPDevice>()
    private val mCustomQuery =
        """
        M-SEARCH * HTTP/1.1
        ST: ssdp:all
        HOST: 239.255.255.250:1900
        MX: 3
        MAN: "ssdp:discover"


        """.trimIndent() // should have two empty lines, otherwise some TV OS can not recognize it

    private const val mInternetAddress: String = "239.255.255.250"
    private const val mPort: Int = 1900

    fun search(context: Context): Flow<UPnPDevice> {
        return callbackFlow {
            devices.forEach {
                trySend(it)
            }
            val wifi =
                context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val lock = wifi.createMulticastLock("The Lock")
            lock.acquire()
            var socket: MulticastSocket? = null
            val group = InetAddress.getByName(mInternetAddress)
            try {
                socket = MulticastSocket()
                socket.reuseAddress = true
                socket.joinGroup(group)
                socket.setReceiveBufferSize(32768)
                socket.broadcast = true
                val datagramPacketRequest =
                    DatagramPacket(mCustomQuery.toByteArray(), mCustomQuery.length, group, mPort)
                socket.send(datagramPacketRequest)
                socket.soTimeout = 5000
                while (isActive) {
                    val datagramPacket = DatagramPacket(ByteArray(1024), 1024)
                    socket.receive(datagramPacket)
                    val response = String(datagramPacket.data, 0, datagramPacket.length)
                    val prefix =
                        response.substring(0, 20)
                            .uppercase(Locale.getDefault())
                    if (prefix.startsWith("HTTP/1.1 200") || prefix.startsWith("NOTIFY * HTTP")) {
                        val device = UPnPDevice(datagramPacket.address.hostAddress!!, response)
                        if (devices.any { it.hostAddress == device.hostAddress }) {
                            continue
                        }
                        LogCat.d(response)
                        devices.add(device)
                        trySend(device)
                    }
                }
            } catch (e: Exception) {
                LogCat.e(e)
                e.printStackTrace()
            } finally {
            }
            awaitClose {
                try {
                    socket?.leaveGroup(group)
                    socket?.close()
                } catch (e: Exception) {
                    LogCat.e(e)
                }
                lock.release()
            }
        }
    }
}
