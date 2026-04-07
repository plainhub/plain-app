package com.ismartcoding.plain.web

import android.content.Context
import android.net.wifi.WifiManager
import com.ismartcoding.lib.helpers.NetworkHelper
import com.ismartcoding.lib.logcat.LogCat
import java.net.DatagramPacket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.MulticastSocket
import java.net.NetworkInterface
import java.net.SocketTimeoutException

/**
 * Lightweight mDNS responder for hostname-to-IPv4 resolution (A record).
 * Keeps local hostname pingable (e.g. plainapp.local) without external libs.
 */
object MdnsHostResponder {
    private const val MDNS_GROUP = "224.0.0.251"
    private const val MDNS_PORT = 5353

    @Volatile
    private var hostname = "plainapp.local"

    @Volatile
    private var worker: Thread? = null

    @Volatile
    private var socket: MulticastSocket? = null

    @Volatile
    private var multicastLock: WifiManager.MulticastLock? = null

    fun start(context: Context, mdnsHostname: String): Boolean {
        val normalized = normalizeHostname(mdnsHostname)
        if (normalized.isEmpty()) {
            LogCat.e("mDNS responder start skipped: empty hostname")
            return false
        }

        stop()
        hostname = normalized

        val group = InetAddress.getByName(MDNS_GROUP)
        val lock = acquireMulticastLock(context)

        return runCatching {
            val s = MulticastSocket(null).apply {
                reuseAddress = true
                soTimeout = 1000
                bind(InetSocketAddress(MDNS_PORT))
                timeToLive = 255
                joinGroup(group)
            }
            socket = s
            multicastLock = lock

            worker = Thread {
                runLoop(s, group)
            }.apply {
                name = "plain-mdns-responder"
                isDaemon = true
                start()
            }
            LogCat.d("mDNS responder started for $hostname")
            true
        }.getOrElse {
            lock?.let { l -> runCatching { l.release() } }
            LogCat.e("Failed to start mDNS responder: ${it.message}")
            false
        }
    }

    fun stop() {
        val t = worker
        worker = null

        val s = socket
        socket = null
        runCatching { s?.close() }

        runCatching { t?.join(300) }

        multicastLock?.let { lock ->
            runCatching {
                if (lock.isHeld) lock.release()
            }
        }
        multicastLock = null
    }

    private fun runLoop(socket: MulticastSocket, group: InetAddress) {
        val buffer = ByteArray(1500)
        while (Thread.currentThread().isAlive) {
            val packet = DatagramPacket(buffer, buffer.size)
            try {
                socket.receive(packet)
                val response = MdnsPacketCodec.buildResponseIfMatch(
                    query = packet.data.copyOf(packet.length),
                    hostname = hostname,
                    ips = getResponderIps(),
                )
                if (response != null) {
                    socket.send(DatagramPacket(response, response.size, group, MDNS_PORT))
                }
            } catch (_: SocketTimeoutException) {
                // timeout keeps the thread responsive to stop()
            } catch (_: Exception) {
                if (worker == null) break
            }
        }
    }

    private fun getResponderIps(): List<Inet4Address> {
        return NetworkHelper.getDeviceIP4s()
            .mapNotNull { ip -> runCatching { InetAddress.getByName(ip) }.getOrNull() }
            .filterIsInstance<Inet4Address>()
            .filter { ip ->
                val ni = runCatching { NetworkInterface.getByInetAddress(ip) }.getOrNull()
                ni != null && !NetworkHelper.isVpnInterface(ni.name) && !ip.isLoopbackAddress
            }
            .distinctBy { it.hostAddress }
    }

    private fun normalizeHostname(value: String): String {
        val trimmed = value.trim().trim('.').lowercase()
        if (trimmed.isEmpty()) return ""
        return if (trimmed.endsWith(".local")) trimmed else "$trimmed.local"
    }

    private fun acquireMulticastLock(context: Context): WifiManager.MulticastLock? {
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            ?: return null
        return runCatching {
            wifi.createMulticastLock("plain-mdns-lock").apply {
                setReferenceCounted(false)
                acquire()
            }
        }.getOrNull()
    }
}