package com.ismartcoding.plain.chat.discover

import android.content.Context
import android.net.wifi.WifiManager
import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.MainApp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.MulticastSocket
import java.net.SocketAddress
import java.net.SocketTimeoutException

/**
 * Low-level UDP transport for nearby device discovery and pairing.
 *
 * Consolidates multicast sending/receiving (with WiFi multicast lock)
 * and unicast sending into a single object.
 */
object NearbyNetwork {
    const val PORT = 52352
    private const val MULTICAST_ADDRESS = "224.0.0.100"
    private const val RECEIVE_TIMEOUT_MS = 10_000
    private const val BUFFER_SIZE = 2048
    private const val RESTART_DELAY_MS = 2000L

    private var receiverJob: Job? = null

    // ---- Sending ---------------------------------------------------------------

    /** Send [message] to the local-subnet multicast group (fire-and-forget). */
    fun sendMulticast(message: String) {
        coIO {
            var socket: MulticastSocket? = null
            try {
                socket = MulticastSocket()
                socket.timeToLive = 1 // limit to local subnet
                val address = InetAddress.getByName(MULTICAST_ADDRESS)
                val bytes = message.toByteArray()
                socket.send(DatagramPacket(bytes, bytes.size, address, PORT))
            } catch (e: Exception) {
                LogCat.e("Multicast send error: ${e.message}")
            } finally {
                socket?.close()
            }
        }
    }

    /** Send [message] to a specific [targetIP] via unicast (fire-and-forget). */
    fun sendUnicast(message: String, targetIP: String) {
        coIO {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket()
                val address = InetAddress.getByName(targetIP)
                val bytes = message.toByteArray()
                socket.send(DatagramPacket(bytes, bytes.size, address, PORT))
            } catch (e: Exception) {
                LogCat.e("Unicast send error to $targetIP: ${e.message}")
            } finally {
                socket?.close()
            }
        }
    }

    // ---- Receiving -------------------------------------------------------------

    /**
     * Start the multicast receiver loop.
     *
     * Acquires a [WifiManager.MulticastLock] so Android forwards multicast traffic
     * to this app, then continuously listens on [PORT]. Automatically restarts on
     * transient socket errors.
     *
     * @param onMessage called for every incoming datagram with (message, senderIP).
     */
    fun startReceiver(onMessage: (message: String, senderIP: String) -> Unit) {
        if (receiverJob?.isActive == true) return

        receiverJob = coIO {
            while (isActive) {
                try {
                    receiveLoop(onMessage)
                } catch (e: Exception) {
                    LogCat.e("Multicast receiver error: ${e.message}")
                }
                if (isActive) {
                    LogCat.d("Multicast receiver restarting in ${RESTART_DELAY_MS}ms…")
                    delay(RESTART_DELAY_MS)
                }
            }
        }
        LogCat.d("Multicast receiver started")
    }

    /** Stop the multicast receiver loop. */
    fun stopReceiver() {
        receiverJob?.cancel()
        receiverJob = null
        LogCat.d("Multicast receiver stopped")
    }

    // ---- Internal --------------------------------------------------------------

    /**
     * One iteration of the receive loop: acquire lock → open socket → listen until
     * error or cancellation → clean up. The caller restarts this when it returns.
     */
    private suspend fun receiveLoop(
        onMessage: (message: String, senderIP: String) -> Unit,
    ) {
        val wifiManager = MainApp.instance.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as WifiManager
        val lock = wifiManager.createMulticastLock("PlainApp:discover")
        lock.setReferenceCounted(false)
        lock.acquire()

        var socket: MulticastSocket? = null
        try {
            socket = MulticastSocket(null as SocketAddress?).apply {
                reuseAddress = true
                soTimeout = RECEIVE_TIMEOUT_MS
            }
            socket.bind(InetSocketAddress(PORT))

            val group = InetAddress.getByName(MULTICAST_ADDRESS)
            socket.joinGroup(group)

            val buffer = ByteArray(BUFFER_SIZE)
            while (receiverJob?.isActive == true) {
                try {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                    val message = String(packet.data, 0, packet.length)
                    val senderIP = packet.address.hostAddress ?: ""
                    onMessage(message, senderIP)
                } catch (_: SocketTimeoutException) {
                    // expected — keep listening
                }
            }
        } finally {
            runCatching {
                socket?.leaveGroup(InetAddress.getByName(MULTICAST_ADDRESS))
                socket?.close()
            }
            runCatching {
                if (lock.isHeld) lock.release()
            }
        }
    }
}
