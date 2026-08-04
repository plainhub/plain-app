@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.ismartcoding.plain.platform

import com.ismartcoding.plain.helpers.coIO
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.lib.logcat.LogCat
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import platform.posix.AF_INET
import platform.posix.IPPROTO_IP
import platform.posix.IP_ADD_MEMBERSHIP
import platform.posix.IP_DROP_MEMBERSHIP
import platform.posix.IP_MULTICAST_IF
import platform.posix.IP_MULTICAST_TTL
import platform.posix.SOCK_DGRAM
import platform.posix.SOL_SOCKET
import platform.posix.SO_REUSEADDR
import platform.posix.SO_RCVTIMEO
import platform.posix.bind
import platform.posix.close
import platform.posix.ip_mreq
import platform.posix.recvfrom
import platform.posix.sendto
import platform.posix.setsockopt
import platform.posix.sockaddr
import platform.posix.sockaddr_in
import platform.posix.socket
import platform.posix.timeval

private const val NEARBY_PORT = 52352
private const val NEARBY_MULTICAST_ADDRESS = "224.0.0.100"
private const val NEARBY_RECEIVE_TIMEOUT_MS = 10_000L
private const val NEARBY_BUFFER_SIZE = 2048
private const val NEARBY_RESTART_DELAY_MS = 2000L

private var nearbyReceiverJob: Job? = null

actual fun nearbySendMulticast(message: String) {
    coIO {
        val localIP = getDeviceIP4()
        if (localIP.isEmpty()) {
            LogCat.e("NearbyNetwork multicast send: no LAN interface")
            return@coIO
        }
        val fd = socket(AF_INET, SOCK_DGRAM, 0)
        if (fd < 0) {
            LogCat.e("NearbyNetwork multicast send: socket() failed")
            return@coIO
        }
        try {
            memScoped {
                val ttl = alloc<IntVar>()
                ttl.value = 1
                setsockopt(fd, IPPROTO_IP, IP_MULTICAST_TTL, ttl.ptr, sizeOf<IntVar>().toUInt())
                val ifAddr = alloc<UIntVar>()
                ifAddr.value = parseIpv4(localIP) ?: 0u
                setsockopt(fd, IPPROTO_IP, IP_MULTICAST_IF, ifAddr.ptr, sizeOf<UIntVar>().toUInt())
                val addr = alloc<sockaddr_in>()
                fillSockaddrIn(addr, NEARBY_MULTICAST_ADDRESS, NEARBY_PORT)
                val bytes = message.encodeToByteArray()
                bytes.usePinned { pinned ->
                    val sent = sendto(
                        fd, pinned.addressOf(0), bytes.size.toULong(), 0,
                        addr.ptr.reinterpret(), sizeOf<sockaddr_in>().toUInt(),
                    )
                    if (sent < 0) {
                        LogCat.e("NearbyNetwork multicast send: sendto() failed")
                    }
                }
            }
        } finally {
            close(fd)
        }
    }
}

actual fun nearbySendUnicast(message: String, targetIP: String) {
    coIO {
        val fd = socket(AF_INET, SOCK_DGRAM, 0)
        if (fd < 0) {
            LogCat.e("NearbyNetwork unicast send to $targetIP: socket() failed")
            return@coIO
        }
        try {
            val bytes = message.encodeToByteArray()
            memScoped {
                val addr = alloc<sockaddr_in>()
                fillSockaddrIn(addr, targetIP, NEARBY_PORT)
                bytes.usePinned { pinned ->
                    val sent = sendto(
                        fd, pinned.addressOf(0), bytes.size.toULong(), 0,
                        addr.ptr.reinterpret(), sizeOf<sockaddr_in>().toUInt(),
                    )
                    if (sent < 0) {
                        LogCat.e("NearbyNetwork unicast send to $targetIP: sendto() failed")
                    }
                }
            }
        } finally {
            close(fd)
        }
    }
}

actual fun nearbyStartReceiver(onMessage: (message: String, senderIP: String) -> Unit) {
    if (nearbyReceiverJob?.isActive == true) return
    nearbyReceiverJob = coIO {
        while (isActive) {
            try {
                nearbyReceiveLoop(onMessage)
            } catch (e: Exception) {
                LogCat.e("NearbyNetwork receiver error: ${e.message}")
            }
            delay(NEARBY_RESTART_DELAY_MS)
        }
    }
    LogCat.d("NearbyNetwork receiver started")
}

actual fun nearbyStopReceiver() {
    nearbyReceiverJob?.cancel()
    nearbyReceiverJob = null
    LogCat.d("NearbyNetwork receiver stopped")
}

private suspend fun nearbyReceiveLoop(onMessage: (message: String, senderIP: String) -> Unit) = withIO {
    if (!hasLanInterface()) return@withIO
    val fd = socket(AF_INET, SOCK_DGRAM, 0)
    if (fd < 0) {
        LogCat.e("NearbyNetwork receiver: socket() failed")
        return@withIO
    }
    var joined = false
    try {
        if (!setupReceiverSocket(fd)) {
            LogCat.e("NearbyNetwork receiver: setup failed")
            return@withIO
        }
        joined = joinMulticastGroup(fd)
        if (!joined) {
            LogCat.e("NearbyNetwork receiver: joinGroup failed")
            return@withIO
        }
        val buffer = ByteArray(NEARBY_BUFFER_SIZE)
        while (nearbyReceiverJob?.isActive == true) {
            val received = recvOne(fd, buffer) ?: continue
            val (host, response) = received
            onMessage(response, host)
        }
    } finally {
        if (joined) leaveMulticastGroup(fd)
        close(fd)
    }
}

private fun setupReceiverSocket(fd: Int): Boolean = memScoped {
    val on = alloc<IntVar>()
    on.value = 1
    if (setsockopt(fd, SOL_SOCKET, SO_REUSEADDR, on.ptr, sizeOf<IntVar>().toUInt()) < 0) {
        LogCat.e("NearbyNetwork: SO_REUSEADDR failed")
    }
    val tv = alloc<timeval>()
    tv.tv_sec = (NEARBY_RECEIVE_TIMEOUT_MS / 1000).convert()
    tv.tv_usec = 0.convert()
    setsockopt(fd, SOL_SOCKET, SO_RCVTIMEO, tv.ptr, sizeOf<timeval>().toUInt())
    val addr = alloc<sockaddr_in>()
    addr.sin_family = AF_INET.convert()
    addr.sin_port = htons(NEARBY_PORT.convert())
    addr.sin_addr.s_addr = 0u
    if (bind(fd, addr.ptr.reinterpret(), sizeOf<sockaddr_in>().toUInt()) < 0) {
        LogCat.e("NearbyNetwork: bind() failed")
        return false
    }
    true
}

private fun joinMulticastGroup(fd: Int): Boolean = memScoped {
    val mreq = alloc<ip_mreq>()
    mreq.imr_multiaddr.s_addr = parseIpv4(NEARBY_MULTICAST_ADDRESS) ?: 0u
    val localIP = getDeviceIP4()
    mreq.imr_interface.s_addr = if (localIP.isNotEmpty()) parseIpv4(localIP) ?: 0u else 0u
    setsockopt(fd, IPPROTO_IP, IP_ADD_MEMBERSHIP, mreq.ptr, sizeOf<ip_mreq>().toUInt()) == 0
}

private fun leaveMulticastGroup(fd: Int) = memScoped {
    val mreq = alloc<ip_mreq>()
    mreq.imr_multiaddr.s_addr = parseIpv4(NEARBY_MULTICAST_ADDRESS) ?: 0u
    val localIP = getDeviceIP4()
    mreq.imr_interface.s_addr = if (localIP.isNotEmpty()) parseIpv4(localIP) ?: 0u else 0u
    setsockopt(fd, IPPROTO_IP, IP_DROP_MEMBERSHIP, mreq.ptr, sizeOf<ip_mreq>().toUInt())
}

private fun recvOne(fd: Int, buffer: ByteArray): Pair<String, String>? = memScoped {
    val srcAddr = alloc<sockaddr_in>()
    val srcLen = alloc<UIntVar>()
    srcLen.value = sizeOf<sockaddr_in>().toUInt()
    val n = buffer.usePinned { pinned ->
        recvfrom(fd, pinned.addressOf(0), buffer.size.toULong(), 0, srcAddr.ptr.reinterpret(), srcLen.ptr)
    }
    if (n <= 0) return@memScoped null
    val len = n.toInt()
    val response = buffer.decodeToString(0, len)
    val host = formatIpv4(srcAddr.sin_addr.s_addr)
    host to response
}

private fun hasLanInterface(): Boolean {
    val ips = getDeviceIP4s()
    return ips.any { !it.startsWith("127.") }
}
