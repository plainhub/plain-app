@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.ismartcoding.plain.mdns

import com.ismartcoding.plain.platform.IosPlatformRegistry
import com.ismartcoding.plain.platform.formatIpv4
import com.ismartcoding.plain.platform.htonl
import com.ismartcoding.plain.platform.htons
import com.ismartcoding.plain.platform.ntohs
import com.ismartcoding.plain.platform.parseIpv4
import kotlin.concurrent.AtomicInt
import kotlin.concurrent.Volatile
import kotlin.native.concurrent.Worker
import kotlin.time.TimeSource
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.UIntVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.set
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.value
import platform.posix.AF_INET
import platform.posix.IPPROTO_IP
import platform.posix.IP_ADD_MEMBERSHIP
import platform.posix.IP_MULTICAST_IF
import platform.posix.SOCK_DGRAM
import platform.posix.SOL_SOCKET
import platform.posix.SO_RCVBUF
import platform.posix.SO_RCVTIMEO
import platform.posix.SO_REUSEADDR
import platform.posix.bind
import platform.posix.close
import platform.posix.if_nametoindex
import platform.posix.ip_mreq
import platform.posix.recvfrom
import platform.posix.sendto
import platform.posix.setsockopt
import platform.posix.sockaddr_in
import platform.posix.socket
import platform.posix.timeval

private const val MDNS_GROUP = "224.0.0.251"

internal actual fun createMdnsSocket(): MdnsSocket = PosixMdnsSocket()

/**
 * iOS actual: enumerates IPv4 interfaces via the Swift-bridged
 * [IosPlatformRegistry.getDeviceIP4s]. Interface names and prefix
 * lengths are synthesized; the first IP always uses "en0" (iOS's
 * primary Wi‑Fi interface) so that `IP_MULTICAST_IF` routing via
 * [if_nametoindex] actually works.
 *
 * Real per-interface data would require calling `getifaddrs` from
 * Swift, which is a minor follow-up enhancement to the
 * IosNetworkInfoProvider bridge — the current approach is chosen to
 * keep platform lowest-level code in Swift (per project conventions)
 * and to avoid an extra C-interop .def for a single Darwin header.
 */
internal actual fun candidateInterfaces(): List<Pair<MdnsIface, String>> {
    val ips = IosPlatformRegistry.getDeviceIP4s()
    val out = ArrayList<Pair<MdnsIface, String>>(ips.size)
    for ((i, ip) in ips.withIndex()) {
        val parts = ip.split(".")
        if (parts.size != 4) continue
        if (parts[0] == "127") continue
        val name = if (i == 0) "en0" else "en$i"
        if (isMobileDataInterface(name)) continue
        out.add(MdnsIface(name, 24) to ip)
    }
    return out
}

internal actual fun startMdnsWorker(name: String, block: () -> Unit): MdnsWorkerHandle {
    val alive = AtomicInt(1)
    val blockRef = StableRef.create(block)
    val worker = Worker.start(name = name)
    worker.executeAfter(0L) {
        try {
            blockRef.get().invoke()
        } finally {
            alive.value = 0
            blockRef.dispose()
        }
    }
    return object : MdnsWorkerHandle {
        override val isAlive get() = alive.value != 0
        override fun join(timeoutMs: Long) {
            val start = TimeSource.Monotonic.markNow()
            while (alive.value != 0) {
                if (start.elapsedNow().inWholeMilliseconds >= timeoutMs) break
                platform.posix.usleep(10_000u)
            }
        }
    }
}

// ── Socket ──────────────────────────────────────────────────────────────────

private class PosixMdnsSocket : MdnsSocket {
    private val fd: Int
    @Volatile private var closed = false
    private var joined = false

    init {
        fd = socket(AF_INET, SOCK_DGRAM, 0).also {
            check(it >= 0) { "mDNS: socket() failed" }
        }
        setSockOptInt(fd, SOL_SOCKET, SO_REUSEADDR, 1)
        setSockOptInt(fd, SOL_SOCKET, SO_RCVBUF, 1 shl 17)
    }

    override val isClosed get() = closed || fd < 0

    override fun bind(port: Int, timeoutMs: Int) {
        memScoped {
            val addr = alloc<sockaddr_in>()
            addr.sin_family = AF_INET.toUByte()
            addr.sin_port = htons(port.toUShort())
            addr.sin_addr.s_addr = htonl(0u)
            check(bind(fd, addr.ptr.reinterpret(), sizeOf<sockaddr_in>().toUInt()) == 0) {
                "mDNS: bind($port) failed"
            }
        }
        if (timeoutMs > 0) {
            memScoped {
                val tv = alloc<timeval>()
                tv.tv_sec = (timeoutMs / 1000).convert()
                tv.tv_usec = ((timeoutMs % 1000) * 1000).convert()
                setsockopt(fd, SOL_SOCKET, SO_RCVTIMEO, tv.ptr, sizeOf<timeval>().toUInt())
            }
        }
    }

    override fun joinGroup(groupIp: String, ifaceName: String?) {
        if (ifaceName != null) setOutgoingInterface(ifaceName)
        if (joined) return
        val groupAddr = parseIpv4(groupIp) ?: return
        memScoped {
            val mreq = alloc<ip_mreq>()
            mreq.imr_multiaddr.s_addr = groupAddr
            mreq.imr_interface.s_addr = htonl(0u)
            check(setsockopt(fd, IPPROTO_IP, IP_ADD_MEMBERSHIP,
                mreq.ptr, sizeOf<ip_mreq>().toUInt()) == 0) {
                "mDNS: IP_ADD_MEMBERSHIP failed"
            }
        }
        joined = true
    }

    override fun setOutgoingInterface(ifaceName: String) {
        val ifIndex = if_nametoindex(ifaceName)
        if (ifIndex == 0u) return
        memScoped {
            val idx = allocArray<ByteVar>(4)
            idx[0] = (ifIndex and 0xFFu).toByte()
            idx[1] = ((ifIndex shr 8) and 0xFFu).toByte()
            idx[2] = ((ifIndex shr 16) and 0xFFu).toByte()
            idx[3] = ((ifIndex shr 24) and 0xFFu).toByte()
            setsockopt(fd, IPPROTO_IP, IP_MULTICAST_IF, idx, 4.toUInt())
        }
    }

    override fun receive(buf: ByteArray): ReceiveResult? = memScoped {
        val src = alloc<sockaddr_in>()
        val srcLen = alloc<UIntVar>()
        srcLen.value = sizeOf<sockaddr_in>().toUInt()
        val tmp = allocArray<ByteVar>(buf.size)
        val n = recvfrom(fd, tmp, buf.size.toULong(), 0, src.ptr.reinterpret(), srcLen.ptr)
        if (n <= 0) return@memScoped null
        val len = n.toInt()
        for (i in 0 until len) buf[i] = tmp[i]
        val ip = formatIpv4(src.sin_addr.s_addr)
        val port = ntohs(src.sin_port).toInt()
        ReceiveResult(len, ip, port)
    }

    override fun send(bytes: ByteArray, destIp: String, destPort: Int) {
        val addrValue = parseIpv4(destIp) ?: return
        memScoped {
            val dst = alloc<sockaddr_in>()
            dst.sin_family = AF_INET.toUByte()
            dst.sin_port = htons(destPort.toUShort())
            dst.sin_addr.s_addr = addrValue
            val tmp = allocArray<ByteVar>(bytes.size)
            for (i in bytes.indices) tmp[i] = bytes[i]
            sendto(fd, tmp, bytes.size.toULong(), 0, dst.ptr.reinterpret(),
                sizeOf<sockaddr_in>().toUInt())
        }
    }

    override fun close() {
        if (!closed) { closed = true; close(fd) }
    }
}

// ── Helpers ─────────────────────────────────────────────────────────────────

private fun setSockOptInt(fd: Int, level: Int, opt: Int, value: Int) = memScoped {
    val v = alloc<IntVar>()
    v.value = value
    setsockopt(fd, level, opt, v.ptr, sizeOf<IntVar>().toUInt())
}
