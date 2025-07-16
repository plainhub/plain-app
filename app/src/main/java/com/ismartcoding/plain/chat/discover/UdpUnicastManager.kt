package com.ismartcoding.plain.chat.discover

import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.lib.logcat.LogCat
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

object UdpUnicastManager {
    fun sendUnicast(message: String, targetIP: String, targetPort: Int) {
        coIO {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket()
                
                val targetAddress = InetAddress.getByName(targetIP)
                val messageBytes = message.toByteArray()
                val packet = DatagramPacket(
                    messageBytes,
                    messageBytes.size,
                    targetAddress,
                    targetPort
                )

                socket.send(packet)
                LogCat.d("Unicast sent to $targetIP:$targetPort: $message")
            } catch (e: Exception) {
                LogCat.e("Error sending unicast to $targetIP:$targetPort: ${e.message}")
            } finally {
                socket?.close()
            }
        }
    }
}