package com.ismartcoding.plain.mdns

import android.content.Context
import android.net.wifi.WifiManager
import com.ismartcoding.lib.logcat.LogCat
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.MulticastSocket
import java.net.SocketTimeoutException

/**
 * Lightweight mDNS responder — single receive socket, per-packet unicast reply.
 *
 * RECEIVE: One MulticastSocket bound to 0.0.0.0:5353 joins 224.0.0.251 on every
 * valid LAN interface (wlan0 Wi-Fi, ap0/wlan1 hotspot, both when active).
 * A single socket avoids the Linux SO_REUSEPORT limitation.
 *
 * SEND: For each query the candidate interface list is re-fetched fresh (never
 * cached), then a throwaway DatagramSocket bound to localIp:0 sends a unicast
 * reply to the querier's source IP. Binding to a specific local IP forces the
 * kernel to route the packet via the interface that owns localIp — no
 * IP_MULTICAST_IF mutation on the shared receive socket is needed.
 *
 * Restart lifecycle: MdnsReregistrar (ConnectivityManager) + MdnsHotspotWatcher
 * (WIFI_AP_STATE_CHANGED) recreate the socket whenever the active interface set
 * changes, keeping receive memberships current.
 */
object MdnsHostResponder {
    private const val MDNS_GROUP = "224.0.0.251"
    private const val MDNS_PORT = 5353

    @Volatile private var hostname = "plainapp.local"

    private val stateLock = Any()
    private var socket: MulticastSocket? = null
    private var worker: Thread? = null
    private var multicastLock: WifiManager.MulticastLock? = null

    fun start(context: Context, mdnsHostname: String): Boolean {
        val normalized = normalizeHostname(mdnsHostname)
        if (normalized.isEmpty()) {
            LogCat.e("mDNS start skipped: empty hostname")
            return false
        }
        stop()
        hostname = normalized

        val candidates = candidateInterfaces()
        if (candidates.isEmpty()) {
            LogCat.e("mDNS: no candidate interfaces found")
            return false
        }

        val multicastGroup = InetAddress.getByName(MDNS_GROUP)
        val groupSockAddr = InetSocketAddress(multicastGroup, MDNS_PORT)
        synchronized(stateLock) {
            val lock = acquireMulticastLock(context)
            val s = runCatching {
                MulticastSocket(null).apply {
                    reuseAddress = true
                    soTimeout = 1000
                    // Explicitly bind to the IPv4 wildcard.
                    // InetSocketAddress(port) resolves to the system-preferred wildcard, which on
                    // Samsung Android 13+ (preferIPv6Addresses=true) becomes [::]:5353 — an IPv6
                    // socket. Joining an IPv4 multicast group on an IPv6 socket silently fails,
                    // leaving this socket unable to receive any mDNS queries.
                    bind(InetSocketAddress(InetAddress.getByName("0.0.0.0"), MDNS_PORT))
                    var joinCount = 0
                    for ((iface, ip) in candidates) {
                        runCatching { joinGroup(groupSockAddr, iface) }
                            .onSuccess { joinCount++; LogCat.d("mDNS joined ${iface.name} (${ip.hostAddress})") }
                            .onFailure { LogCat.e("mDNS joinGroup ${iface.name}: ${it.message}") }
                    }
                    if (joinCount == 0) {
                        LogCat.e("mDNS: no interface joined multicast group — responder will not receive queries")
                    }
                }
            }.getOrElse {
                lock?.let { l -> runCatching { l.release() } }
                LogCat.e("mDNS socket create failed: ${it.message}")
                return false
            }
            socket = s
            multicastLock = lock
            worker = Thread { runLoop(s) }.apply {
                name = "plain-mdns-responder"
                isDaemon = true
                start()
            }
        }
        LogCat.d("mDNS responder started for $hostname on ${candidates.size} interface(s)")
        return true
    }

    fun stop() {
        synchronized(stateLock) {
            val t = worker; worker = null
            val s = socket; socket = null
            runCatching { s?.close() }
            runCatching { t?.join(300) }
            multicastLock?.let { ml -> runCatching { if (ml.isHeld) ml.release() } }
            multicastLock = null
        }
    }

    private fun runLoop(s: MulticastSocket) {
        val buf = ByteArray(1500)
        while (!s.isClosed) {
            val packet = DatagramPacket(buf, buf.size)
            try {
                s.receive(packet)
                // extractInet4Address handles both plain Inet4Address and IPv4-mapped IPv6
                // addresses (::ffff:x.x.x.x) that a dual-stack socket may report.
                val senderIp = extractInet4Address(packet.address) ?: continue
                val fresh = candidateInterfaces()
                if (fresh.isEmpty()) continue
                val (_, localIp) = findResponseIface(senderIp, fresh)
                val response = MdnsPacketCodec.buildResponseIfMatch(
                    query = packet.data.copyOf(packet.length),
                    hostname = hostname,
                    ips = listOf(localIp),
                ) ?: continue
                sendUnicast(response, localIp, senderIp)
            } catch (_: SocketTimeoutException) {
                // expected — keeps thread responsive to socket close
            } catch (_: Exception) {
                if (s.isClosed) break
            }
        }
    }

    internal fun sendUnicast(response: ByteArray, localIp: Inet4Address, dest: Inet4Address) {
        runCatching {
            DatagramSocket(InetSocketAddress(localIp, 0)).use { ds ->
                ds.send(DatagramPacket(response, response.size, dest, MDNS_PORT))
            }
        }.onFailure { LogCat.e("mDNS send to ${dest.hostAddress}: ${it.message}") }
    }

    /**
     * Returns the IPv4 address from [addr], unwrapping IPv4-mapped IPv6 addresses
     * (::ffff:x.x.x.x) that a dual-stack socket reports for IPv4 senders.
     */
    internal fun extractInet4Address(addr: InetAddress): Inet4Address? {
        if (addr is Inet4Address) return addr
        if (addr is Inet6Address) {
            val b = addr.address
            // IPv4-mapped format: 10 zero bytes + 0xFF 0xFF + 4 IPv4 bytes
            if (b.size == 16 && b[0] == 0.toByte() && b[10] == 0xFF.toByte() && b[11] == 0xFF.toByte() &&
                b.take(10).all { it == 0.toByte() }
            ) {
                return runCatching { InetAddress.getByAddress(b.copyOfRange(12, 16)) as? Inet4Address }.getOrNull()
            }
        }
        return null
    }

    internal fun normalizeHostname(value: String): String {
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