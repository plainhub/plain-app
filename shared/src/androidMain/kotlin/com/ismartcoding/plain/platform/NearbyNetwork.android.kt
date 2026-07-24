package com.ismartcoding.plain.platform

import android.content.Context
import android.net.wifi.WifiManager
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.helpers.coIO
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.lib.logcat.LogCat
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.MulticastSocket
import java.net.NetworkInterface
import java.net.SocketAddress
import java.net.SocketTimeoutException

actual object NearbyNetwork {
    private const val PORT = 52352
    private const val MULTICAST_ADDRESS = "224.0.0.100"
    private const val RECEIVE_TIMEOUT_MS = 10_000
    private const val BUFFER_SIZE = 2048
    private const val RESTART_DELAY_MS = 2000L

    private var receiverJob: Job? = null
    private var multicastLock: WifiManager.MulticastLock? = null

    actual fun sendMulticast(message: String) {
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

    actual fun sendUnicast(message: String, targetIP: String) {
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

    actual fun startReceiver(onMessage: (message: String, senderIP: String) -> Unit) {
        if (receiverJob?.isActive == true) {
            return
        }

        // Acquire a single MulticastLock for the lifetime of the receiver.
        // Creating a new lock on every restart iteration eventually trips
        // Android's per-UID lock cap ("Exceeded maximum number of wifi locks").
        if (multicastLock == null) {
            runCatching {
                val wifiManager = appContext.applicationContext
                    .getSystemService(Context.WIFI_SERVICE) as WifiManager
                multicastLock = wifiManager.createMulticastLock("PlainApp:discover").apply {
                    setReferenceCounted(false)
                    acquire()
                }
            }.onFailure { LogCat.e("Multicast lock acquire error: ${it.message}") }
        } else if (multicastLock?.isHeld == false) {
            runCatching { multicastLock?.acquire() }
        }

        receiverJob = coIO {
            while (isActive) {
                try {
                    receiveLoop(onMessage)
                } catch (e: Exception) {
                    LogCat.e("Multicast receiver error: ${e.message}")
                }
                delay(RESTART_DELAY_MS)
            }
        }
        LogCat.d("Multicast receiver started")
    }

    actual fun stopReceiver() {
        receiverJob?.cancel()
        receiverJob = null
        runCatching {
            if (multicastLock?.isHeld == true) multicastLock?.release()
        }
        multicastLock = null
        LogCat.d("Multicast receiver stopped")
    }

    /**
     * One iteration of the receive loop: open socket → listen until
     * error or cancellation → close socket. The MulticastLock is owned by
     * [startReceiver]/[stopReceiver] and is shared across iterations, so we do
     * NOT create a new lock here (Android caps WifiLocks per UID).
     */
    private suspend fun receiveLoop(
        onMessage: (message: String, senderIP: String) -> Unit,
    ) = withIO {
        // Skip when no LAN interface is up (Wi-Fi off, airplane mode, hotspot-only).
        // joinGroup() would otherwise throw ENODEV and the outer retry loop would
        // spam the log every RESTART_DELAY_MS until an interface appears.
        if (!hasLanInterface()) return@withIO

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
        }
    }

    /**
     * Returns true when at least one up, non-loopback, non-mobile IPv4 interface
     * is available — i.e. a real LAN interface joinGroup() can bind to. Mirrors
     * the candidateInterfaces() check in MdnsHostResponder so the receiver does
     * not attempt IP_ADD_MEMBERSHIP on a kernel with no usable interface, which
     * throws ENODEV ("No such device").
     */
    private fun hasLanInterface(): Boolean {
        return runCatching {
            NetworkInterface.getNetworkInterfaces()
                ?.asSequence()
                ?.filter { it.isUp }
                ?.filterNot { it.isLoopback }
                ?.filterNot { isMobileDataInterface(it.name) }
                ?.any { iface ->
                    iface.inetAddresses.asSequence().any { it is Inet4Address && !it.isLoopbackAddress }
                }
                ?: false
        }.getOrDefault(false)
    }

    private fun isMobileDataInterface(name: String): Boolean =
        name.startsWith("rmnet") || name.startsWith("ccmni") ||
            name.startsWith("v4-rmnet") || name.startsWith("v6-rmnet") ||
            name.startsWith("clat") || name.startsWith("v4-ccmni")
}
