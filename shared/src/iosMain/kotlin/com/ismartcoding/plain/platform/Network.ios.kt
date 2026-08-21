@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.ismartcoding.plain.platform

import kotlinx.cinterop.IntVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.value
import platform.posix.AF_INET
import platform.posix.SOCK_STREAM
import platform.posix.SOL_SOCKET
import platform.posix.SO_REUSEADDR
import platform.posix.bind
import platform.posix.close
import platform.posix.setsockopt
import platform.posix.sockaddr_in
import platform.posix.socket

actual fun getNetworkType(): NetworkType {
    val ips = getDeviceIP4s()
    return if (ips.any { !it.startsWith("127.") }) NetworkType.WIFI else NetworkType.NONE
}

actual fun getDeviceIP4(): String {
    val ips = getDeviceIP4s()
    return ips.firstOrNull { !it.startsWith("127.") } ?: ""
}

actual fun getDeviceIP4sWithPrefixLength(): Set<Pair<String, Short>> {
    val ips = getDeviceIP4s().filter { !it.startsWith("127.") }
    return ips.map { it to 24.toShort() }.toSet()
}

actual fun isVPNConnected(): Boolean {
    val ips = getDeviceIP4s()
    val has10 = ips.any { it.startsWith("10.") }
    val hasNon10 = ips.any { !it.startsWith("127.") && !it.startsWith("10.") }
    return has10 && hasNon10
}

actual fun isPortInUse(port: Int): Boolean {
    val fd = socket(AF_INET, SOCK_STREAM, 0)
    if (fd < 0) return false
    return memScoped {
        try {
            val on = alloc<IntVar>()
            on.value = 1
            setsockopt(fd, SOL_SOCKET, SO_REUSEADDR, on.ptr, sizeOf<IntVar>().toUInt())
            val addr = alloc<sockaddr_in>()
            addr.sin_family = AF_INET.convert()
            addr.sin_port = htons(port.convert())
            addr.sin_addr.s_addr = 0u
            val result = bind(fd, addr.ptr.reinterpret(), sizeOf<sockaddr_in>().toUInt())
            result != 0
        } finally {
            close(fd)
        }
    }
}
