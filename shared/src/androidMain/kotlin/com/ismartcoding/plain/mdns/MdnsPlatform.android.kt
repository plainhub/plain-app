package com.ismartcoding.plain.mdns

import android.content.Context
import android.net.wifi.WifiManager
import java.net.DatagramPacket
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.MulticastSocket
import java.net.NetworkInterface
import java.net.SocketTimeoutException

internal actual fun createMdnsSocket(): MdnsSocket = JvmMdnsSocket()

internal actual fun candidateInterfaces(): List<Pair<MdnsIface, String>> =
    runCatching {
        NetworkInterface.getNetworkInterfaces()?.asSequence()
            ?.filter { it.isUp && !it.isLoopback }
            ?.filterNot { isMobileDataInterface(it.name) }
            ?.mapNotNull { iface ->
                val ip = iface.inetAddresses.asSequence()
                    .filterIsInstance<Inet4Address>()
                    .firstOrNull { !it.isLoopbackAddress } ?: return@mapNotNull null
                val prefix = iface.interfaceAddresses
                    .firstOrNull { it.address == ip }
                    ?.networkPrefixLength?.toShort() ?: 24
                MdnsIface(iface.name, prefix) to ip.hostAddress
            }
            ?.toList() ?: emptyList()
    }.getOrElse { emptyList() }

internal actual fun startMdnsWorker(name: String, block: () -> Unit): MdnsWorkerHandle {
    val t = Thread(block, name).apply { isDaemon = true; start() }
    return object : MdnsWorkerHandle {
        override val isAlive get() = t.isAlive
        override fun join(timeoutMs: Long) { runCatching { t.join(timeoutMs) } }
    }
}

private fun appContext(): Context? = runCatching {
    val app = Class.forName("android.app.ActivityThread")
        .getMethod("currentApplication").invoke(null) as? android.app.Application
    app?.applicationContext
}.getOrNull()

private class JvmMdnsSocket : MdnsSocket {
    private val socket = MulticastSocket(null)
    private val multicastLock = acquireMulticastLock()
    private var boundPort = 0

    override val isClosed get() = socket.isClosed

    override fun bind(port: Int, timeoutMs: Int) {
        socket.reuseAddress = true
        socket.soTimeout = timeoutMs
        socket.bind(InetSocketAddress(InetAddress.getByName("0.0.0.0"), port))
        boundPort = port
    }

    override fun joinGroup(groupIp: String, ifaceName: String?) {
        val group = InetAddress.getByName(groupIp)
        if (ifaceName != null) {
            val iface = NetworkInterface.getByName(ifaceName) ?: return
            socket.joinGroup(InetSocketAddress(group, boundPort), iface)
        } else {
            socket.joinGroup(group)
        }
    }

    override fun setOutgoingInterface(ifaceName: String) {
        NetworkInterface.getByName(ifaceName)?.let { socket.networkInterface = it }
    }

    override fun receive(buf: ByteArray): ReceiveResult? {
        val dp = DatagramPacket(buf, buf.size)
        try {
            socket.receive(dp)
        } catch (_: SocketTimeoutException) {
            return null
        }
        val senderIp = dp.address?.let(::extractInet4)
        return ReceiveResult(dp.length, senderIp, dp.port)
    }

    override fun send(bytes: ByteArray, destIp: String, destPort: Int) {
        socket.send(DatagramPacket(bytes, bytes.size, InetAddress.getByName(destIp), destPort))
    }

    override fun close() {
        runCatching { socket.close() }
        runCatching { multicastLock?.let { if (it.isHeld) it.release() } }
    }

    private fun acquireMulticastLock(): WifiManager.MulticastLock? {
        val ctx = appContext() ?: return null
        val wifi = ctx.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return null
        return runCatching {
            wifi.createMulticastLock("plain-mdns-lock").apply {
                setReferenceCounted(false); acquire()
            }
        }.getOrNull()
    }
}

/** Unwraps IPv4-mapped IPv6 addresses (::ffff:x.x.x.x) from dual-stack sockets. */
private fun extractInet4(addr: InetAddress): String? {
    if (addr is Inet4Address) return addr.hostAddress
    if (addr is Inet6Address) {
        val b = addr.address
        if (b.size == 16 && b[10] == 0xFF.toByte() && b[11] == 0xFF.toByte() &&
            b.take(10).all { it == 0.toByte() }
        ) {
            return "${b[12].toInt() and 0xFF}.${b[13].toInt() and 0xFF}." +
                "${b[14].toInt() and 0xFF}.${b[15].toInt() and 0xFF}"
        }
    }
    return null
}
