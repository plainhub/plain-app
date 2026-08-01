@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.ismartcoding.plain.platform

import com.ismartcoding.plain.lib.logcat.LogCat
import kotlinx.cinterop.CValuesRef
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.launch
import platform.AVFoundation.AVPlayer
import platform.CoreMedia.CMTime
import platform.Foundation.NSInvocation
import platform.Foundation.NSMethodSignature
import platform.darwin.NSObject
import platform.objc.sel_registerName
import platform.posix.AF_INET
import platform.posix.IPPROTO_IP
import platform.posix.IP_ADD_MEMBERSHIP
import platform.posix.SOCK_DGRAM
import platform.posix.SOL_SOCKET
import platform.posix.SO_REUSEADDR
import platform.posix.SO_RCVTIMEO
import platform.posix.bind
import platform.posix.close
import platform.posix.ip_mreq
import platform.posix.memcpy
import platform.posix.recvfrom
import platform.posix.sendto
import platform.posix.setsockopt
import platform.posix.sockaddr
import platform.posix.sockaddr_in
import platform.posix.socket
import platform.posix.timeval

private const val SSDP_ADDR = "239.255.255.250"
private const val SSDP_PORT = 1900
private const val RCV_TIMEOUT_SECS = 5L
private const val SEARCH_QUERY =
    "M-SEARCH * HTTP/1.1\r\nST: ssdp:all\r\nHOST: 239.255.255.250:1900\r\nMX: 3\r\nMAN: \"ssdp:discover\"\r\n\r\n"

actual fun startDlnaRenderer() {
    LogCat.d("DLNA renderer: start requested (iOS — discovery-only, renderer not started)")
}

actual fun stopDlnaRenderer() {
    LogCat.d("DLNA renderer: stop requested (iOS no-op)")
}

actual fun getPlayerPositionMs(player: Any?): Long {
    val avp = player as? AVPlayer ?: return 0L
    return cmTimeSelectorToMs(avp, "currentTime")
}

actual fun getPlayerDurationMs(player: Any?): Long {
    val avp = player as? AVPlayer ?: return 0L
    val item = avp.performSelector(sel_registerName("currentItem")) as? NSObject ?: return 0L
    return cmTimeSelectorToMs(item, "duration")
}

actual fun searchDlnaDevicesRaw(): Flow<DlnaSsdpResponse> = callbackFlow {
    val fd = socket(AF_INET, SOCK_DGRAM, 0)
    if (fd < 0) {
        LogCat.e("DLNA scanner: socket() failed: ${errnoString()}")
        awaitClose { }
        return@callbackFlow
    }

    if (!setupScannerSocket(fd)) {
        close(fd)
        awaitClose { }
        return@callbackFlow
    }

    val queryBytes = SEARCH_QUERY.encodeToByteArray()

    val job = launch(IODispatcher) {
        try {
            memScoped {
                val destAddr = alloc<sockaddr_in>()
                fillSockaddrIn(destAddr, SSDP_ADDR, SSDP_PORT)
                val destPtr = destAddr.ptr.reinterpret<sockaddr>()
                sendMsearch(fd, queryBytes, destPtr)
                LogCat.d("DLNA scanner: sent M-SEARCH to $SSDP_ADDR:$SSDP_PORT")

                val buffer = ByteArray(4096)
                while (isActive) {
                    val received = recvOne(fd, buffer)
                    if (received == null) {
                        sendMsearch(fd, queryBytes, destPtr)
                        continue
                    }
                    val (host, response) = received
                    val prefix = response.take(20).uppercase()
                    if (prefix.startsWith("HTTP/1.1 200") || prefix.startsWith("NOTIFY * HTTP")) {
                        LogCat.d("DLNA scanner: received response from $host")
                        trySend(DlnaSsdpResponse(host, response))
                    }
                }
            }
        } catch (e: Exception) {
            LogCat.e("DLNA scanner error: ${e.message}")
        }
    }

    awaitClose {
        job.cancel()
        close(fd)
        LogCat.d("DLNA scanner: socket closed")
    }
}

private fun cmTimeSelectorToMs(target: NSObject, selName: String): Long {
    return try {
        val sel = sel_registerName(selName)
        val sigRaw = target.methodSignatureForSelector(sel) ?: return 0L
        val sig = sigRaw as Any as NSMethodSignature
        val inv = NSInvocation.invocationWithMethodSignature(sig)
        inv.setTarget(target)
        inv.setSelector(sel)
        inv.invoke()
        val size = sig.methodReturnLength.toInt().coerceAtLeast(24)
        val buf = ByteArray(size)
        buf.usePinned { pinned ->
            inv.getReturnValue(pinned.addressOf(0))
        }
        memScoped {
            val t = alloc<CMTime>()
            buf.usePinned { pinned ->
                memcpy(t.ptr, pinned.addressOf(0), size.toULong())
            }
            if (t.timescale == 0) return@memScoped 0L
            val seconds = t.value.toDouble() / t.timescale.toDouble()
            if (seconds.isNaN() || seconds.isInfinite() || seconds < 0.0) 0L else (seconds * 1000.0).toLong()
        }
    } catch (e: Exception) {
        LogCat.e("cmTimeSelectorToMs($selName) failed: ${e.message}")
        0L
    }
}

private fun setupScannerSocket(fd: Int): Boolean {
    memScoped {
        val on = alloc<IntVar>()
        on.value = 1
        if (setsockopt(fd, SOL_SOCKET, SO_REUSEADDR, on.ptr, sizeOf<IntVar>().toUInt()) < 0) {
            LogCat.e("DLNA scanner: SO_REUSEADDR failed: ${errnoString()}")
        }
    }
    memScoped {
        val addr = alloc<sockaddr_in>()
        addr.sin_family = AF_INET.convert()
        addr.sin_port = 0.convert()
        addr.sin_addr.s_addr = 0u
        if (bind(fd, addr.ptr.reinterpret(), sizeOf<sockaddr_in>().toUInt()) < 0) {
            LogCat.e("DLNA scanner: bind() failed: ${errnoString()}")
            return false
        }
    }
    memScoped {
        val mreq = alloc<ip_mreq>()
        mreq.imr_multiaddr.s_addr = parseIpv4(SSDP_ADDR) ?: 0u
        mreq.imr_interface.s_addr = 0u
        if (setsockopt(fd, IPPROTO_IP, IP_ADD_MEMBERSHIP, mreq.ptr, sizeOf<ip_mreq>().toUInt()) < 0) {
            LogCat.e("DLNA scanner: IP_ADD_MEMBERSHIP failed: ${errnoString()}")
        }
    }
    memScoped {
        val tv = alloc<timeval>()
        tv.tv_sec = RCV_TIMEOUT_SECS.convert()
        tv.tv_usec = 0.convert()
        setsockopt(fd, SOL_SOCKET, SO_RCVTIMEO, tv.ptr, sizeOf<timeval>().toUInt())
    }
    return true
}

private fun sendMsearch(fd: Int, bytes: ByteArray, dest: CValuesRef<sockaddr>) {
    bytes.usePinned { pinned ->
        val sent = sendto(fd, pinned.addressOf(0), bytes.size.toULong(), 0, dest, sizeOf<sockaddr_in>().toUInt())
        if (sent < 0) {
            LogCat.e("DLNA scanner: sendto() failed: ${errnoString()}")
        }
    }
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

private fun errnoString(): String = ""
