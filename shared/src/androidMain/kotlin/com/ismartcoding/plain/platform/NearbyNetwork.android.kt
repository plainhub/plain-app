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

private const val NEARBY_PORT = 52352
private const val NEARBY_MULTICAST_ADDRESS = "224.0.0.100"
private const val NEARBY_RECEIVE_TIMEOUT_MS = 10_000
private const val NEARBY_BUFFER_SIZE = 2048
private const val NEARBY_RESTART_DELAY_MS = 2000L

private var nearbyReceiverJob: Job? = null
private var nearbyMulticastLock: WifiManager.MulticastLock? = null

actual fun nearbySendMulticast(message: String) {
    coIO {
        var socket: MulticastSocket? = null
        try {
            socket = MulticastSocket()
            socket.timeToLive = 1 // limit to local subnet
            val address = InetAddress.getByName(NEARBY_MULTICAST_ADDRESS)
            val bytes = message.toByteArray()
            socket.send(DatagramPacket(bytes, bytes.size, address, NEARBY_PORT))
        } catch (e: Exception) {
            LogCat.e("Multicast send error: ${e.message}")
        } finally {
            socket?.close()
        }
    }
}

actual fun nearbySendUnicast(message: String, targetIP: String) {
    coIO {
        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket()
            val address = InetAddress.getByName(targetIP)
            val bytes = message.toByteArray()
            socket.send(DatagramPacket(bytes, bytes.size, address, NEARBY_PORT))
        } catch (e: Exception) {
            LogCat.e("Unicast send error to $targetIP: ${e.message}")
        } finally {
            socket?.close()
        }
    }
}

actual fun nearbyStartReceiver(onMessage: (message: String, senderIP: String) -> Unit) {
    if (nearbyReceiverJob?.isActive == true) {
        return
    }

    // Acquire a single MulticastLock for the lifetime of the receiver.
    // Creating a new lock on every restart iteration eventually trips
    // Android's per-UID lock cap ("Exceeded maximum number of wifi locks").
    if (nearbyMulticastLock == null) {
        runCatching {
            val wifiManager = appContext.applicationContext
                .getSystemService(Context.WIFI_SERVICE) as WifiManager
            nearbyMulticastLock = wifiManager.createMulticastLock("PlainApp:discover").apply {
                setReferenceCounted(false)
                acquire()
            }
        }.onFailure { LogCat.e("Multicast lock acquire error: ${it.message}") }
    } else if (nearbyMulticastLock?.isHeld == false) {
        runCatching { nearbyMulticastLock?.acquire() }
    }

    nearbyReceiverJob = coIO {
        while (isActive) {
            try {
                nearbyReceiveLoop(onMessage)
            } catch (e: Exception) {
                LogCat.e("Multicast receiver error: ${e.message}")
            }
            delay(NEARBY_RESTART_DELAY_MS)
        }
    }
    LogCat.d("Multicast receiver started")
}

actual fun nearbyStopReceiver() {
    nearbyReceiverJob?.cancel()
    nearbyReceiverJob = null
    runCatching {
        if (nearbyMulticastLock?.isHeld == true) nearbyMulticastLock?.release()
    }
    nearbyMulticastLock = null
    LogCat.d("Multicast receiver stopped")
}

/**
 * One iteration of the receive loop: open socket → listen until
 * error or cancellation → close socket. The MulticastLock is owned by
 * [nearbyStartReceiver]/[nearbyStopReceiver] and is shared across iterations, so we do
 * NOT create a new lock here (Android caps WifiLocks per UID).
 */
private suspend fun nearbyReceiveLoop(
    onMessage: (message: String, senderIP: String) -> Unit,
) = withIO {
    // Skip when no LAN interface is up (Wi-Fi off, airplane mode, hotspot-only).
    // joinGroup() would otherwise throw ENODEV and the outer retry loop would
    // spam the log every NEARBY_RESTART_DELAY_MS until an interface appears.
    if (!hasLanInterface()) return@withIO

    var socket: MulticastSocket? = null
    try {
        socket = MulticastSocket(null as SocketAddress?).apply {
            reuseAddress = true
            soTimeout = NEARBY_RECEIVE_TIMEOUT_MS
        }
        socket.bind(InetSocketAddress(NEARBY_PORT))

        val group = InetAddress.getByName(NEARBY_MULTICAST_ADDRESS)
        socket.joinGroup(group)

        val buffer = ByteArray(NEARBY_BUFFER_SIZE)
        while (nearbyReceiverJob?.isActive == true) {
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
            socket?.leaveGroup(InetAddress.getByName(NEARBY_MULTICAST_ADDRESS))
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
