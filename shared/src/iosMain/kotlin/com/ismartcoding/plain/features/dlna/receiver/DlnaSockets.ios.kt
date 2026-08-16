@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.ismartcoding.plain.features.dlna.receiver

import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.platform.IODispatcher
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.UIntVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import kotlinx.coroutines.withContext
import platform.posix.AF_INET
import platform.posix.IPPROTO_IP
import platform.posix.IP_ADD_MEMBERSHIP
import platform.posix.SOCK_DGRAM
import platform.posix.SOL_SOCKET
import platform.posix.SO_REUSEADDR
import platform.posix.SO_RCVTIMEO
import platform.posix.bind
import platform.posix.close
import platform.posix.in_addr_t
import platform.posix.ip_mreq
import platform.posix.recvfrom
import platform.posix.sendto
import platform.posix.setsockopt
import platform.posix.sockaddr_in
import platform.posix.socket
import platform.posix.timeval

/**
 * iOS actual implementation of [DlnaSsdpSocket] backed by a POSIX `AF_INET`
 * UDP socket bound to `0.0.0.0:1900` and joined to the SSDP multicast group
 * `239.255.255.250`, so it receives both multicast M-SEARCH queries and
 * unicast SSDP traffic. Replies are sent via [sendUnicast] to the querier's
 * source address:port (per UPnP spec).
 */
private class IosDlnaSsdpSocket(private val fd: Int) : DlnaSsdpSocket {

    override suspend fun receive(timeoutMs: Int): DlnaSsdpPacket? = withContext(IODispatcher) {
        memScoped {
            val tv = alloc<timeval>()
            tv.tv_sec = (timeoutMs / 1000).convert()
            tv.tv_usec = ((timeoutMs % 1000) * 1000).convert()
            setsockopt(fd, SOL_SOCKET, SO_RCVTIMEO, tv.ptr, sizeOf<timeval>().toUInt())
        }
        memScoped {
            val srcAddr = alloc<sockaddr_in>()
            val srcLen = alloc<UIntVar>()
            srcLen.value = sizeOf<sockaddr_in>().toUInt()
            val buffer = ByteArray(4096)
            val n = buffer.usePinned { pinned ->
                recvfrom(fd, pinned.addressOf(0), buffer.size.toULong(), 0, srcAddr.ptr.reinterpret(), srcLen.ptr)
            }
            if (n <= 0) return@withContext null
            val len = n.toInt()
            val message = buffer.decodeToString(0, len)
            val sourceAddress = formatIpv4(srcAddr.sin_addr.s_addr)
            val sourcePort = ntohs(srcAddr.sin_port).toInt()
            DlnaSsdpPacket(message, sourceAddress, sourcePort)
        }
    }

    override fun sendMulticast(message: String) {
        sendToAddr(message, DlnaSsdpMessages.SSDP_ADDR, DlnaSsdpMessages.SSDP_PORT)
    }

    override fun sendUnicast(message: String, address: String, port: Int) {
        sendToAddr(message, address, port)
    }

    private fun sendToAddr(message: String, ip: String, port: Int) {
        val bytes = message.encodeToByteArray()
        val addrValue = parseIpv4(ip) ?: run {
            LogCat.e("SSDP send: invalid IPv4 address '$ip'")
            return
        }
        memScoped {
            val dest = alloc<sockaddr_in>()
            dest.sin_family = AF_INET.convert()
            dest.sin_port = htons(port.convert())
            dest.sin_addr.s_addr = addrValue
            bytes.usePinned { pinned ->
                val sent = sendto(fd, pinned.addressOf(0), bytes.size.toULong(), 0, dest.ptr.reinterpret(), sizeOf<sockaddr_in>().toUInt())
                if (sent < 0) {
                    LogCat.e("SSDP sendto($ip:$port) failed: ${errnoString()}")
                }
            }
        }
    }

    override fun close() {
        platform.posix.close(fd)
    }
}

/** Actual factory: creates a POSIX UDP socket bound to 0.0.0.0:[bindPort] joined to the SSDP group. */
actual fun createDlnaSsdpSocket(bindPort: Int): DlnaSsdpSocket? {
    val fd = socket(AF_INET, SOCK_DGRAM, 0)
    if (fd < 0) {
        LogCat.e("createDlnaSsdpSocket: socket() failed: ${errnoString()}")
        return null
    }
    memScoped {
        val on = alloc<IntVar>()
        on.value = 1
        if (setsockopt(fd, SOL_SOCKET, SO_REUSEADDR, on.ptr, sizeOf<IntVar>().toUInt()) < 0) {
            LogCat.e("createDlnaSsdpSocket: SO_REUSEADDR failed: ${errnoString()}")
        }
    }
    memScoped {
        val addr = alloc<sockaddr_in>()
        addr.sin_family = AF_INET.convert()
        addr.sin_port = htons(bindPort.convert())
        addr.sin_addr.s_addr = 0u
        if (bind(fd, addr.ptr.reinterpret(), sizeOf<sockaddr_in>().toUInt()) < 0) {
            LogCat.e("createDlnaSsdpSocket: bind(0.0.0.0:$bindPort) failed: ${errnoString()}")
            close(fd)
            return null
        }
    }
    memScoped {
        val mreq = alloc<ip_mreq>()
        mreq.imr_multiaddr.s_addr = parseIpv4(DlnaSsdpMessages.SSDP_ADDR) ?: 0u
        mreq.imr_interface.s_addr = 0u
        if (setsockopt(fd, IPPROTO_IP, IP_ADD_MEMBERSHIP, mreq.ptr, sizeOf<ip_mreq>().toUInt()) < 0) {
            LogCat.e("createDlnaSsdpSocket: IP_ADD_MEMBERSHIP failed: ${errnoString()}")
            close(fd)
            return null
        }
    }
    LogCat.d("DLNA SSDP socket bound to 0.0.0.0:$bindPort joined ${DlnaSsdpMessages.SSDP_ADDR}")
    return IosDlnaSsdpSocket(fd)
}

private fun parseIpv4(s: String): in_addr_t? {
    val parts = s.split(".")
    if (parts.size != 4) return null
    var host = 0u
    for (p in parts) {
        val n = p.toIntOrNull() ?: return null
        if (n < 0 || n > 255) return null
        host = (host shl 8) or n.toUInt()
    }
    return htonl(host)
}

private fun formatIpv4(netOrder: in_addr_t): String {
    val host = ntohl(netOrder)
    val a = (host shr 24) and 0xFFu
    val b = (host shr 16) and 0xFFu
    val c = (host shr 8) and 0xFFu
    val d = host and 0xFFu
    return "$a.$b.$c.$d"
}

private fun htons(value: UShort): UShort {
    val v = value.toInt() and 0xFFFF
    return (((v and 0xFF) shl 8) or ((v shr 8) and 0xFF)).toUShort()
}

private fun htonl(value: UInt): UInt {
    return ((value and 0xFFu) shl 24) or
        ((value and 0xFF00u) shl 8) or
        ((value and 0xFF0000u) shr 8) or
        ((value and 0xFF000000u) shr 24)
}

private fun ntohs(value: UShort): UShort = htons(value)

private fun ntohl(value: UInt): UInt = htonl(value)

private fun errnoString(): String = ""
