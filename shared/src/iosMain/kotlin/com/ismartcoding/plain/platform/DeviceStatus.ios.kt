package com.ismartcoding.plain.platform

import com.ismartcoding.plain.data.DDeviceStatus
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.toKString
import kotlinx.cinterop.UIntVar
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.get
import kotlinx.cinterop.value
import kotlinx.coroutines.delay
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSystemFreeSize
import platform.Foundation.NSNumber
import platform.Foundation.NSProcessInfo
import platform.UIKit.UIDevice
import platform.UIKit.UIDeviceBatteryState
import platform.darwin.HOST_CPU_LOAD_INFO
import platform.darwin.host_cpu_load_info
import platform.darwin.host_statistics
import platform.darwin.mach_host_self
import platform.posix.uname
import platform.posix.utsname

actual suspend fun getDeviceStatus(): DDeviceStatus = DDeviceStatus().apply {
    uptimeSec = NSProcessInfo.processInfo.systemUptime.toLong()

    UIDevice.currentDevice.batteryMonitoringEnabled = true
    val level = UIDevice.currentDevice.batteryLevel
    if (level >= 0) {
        batteryLevel = (level * 100).toInt().coerceIn(0, 100)
    }
    charging = UIDevice.currentDevice.batteryState == UIDeviceBatteryState.UIDeviceBatteryStateCharging

    val prev = machCpuTimes()
    delay(CPU_SAMPLE_INTERVAL_MS)
    val cur = machCpuTimes()
    if (prev != null && cur != null) {
        cpuUsage = cpuUsagePercent(prev, cur)
    }

    memoryAvailable = null

    storageAvailable = fileSystemFreeSize()
}

/**
 * Aggregated CPU tick counters from HOST_CPU_LOAD_INFO:
 * [user, system, idle, nice] — the struct is exactly 4 x integer_t, so we
 * allocate it as an int array and reinterpret for the call.
 */
@OptIn(ExperimentalForeignApi::class)
private fun machCpuTimes(): CpuTimes? = memScoped {
    val count = alloc<UIntVar>()
    count.value = (sizeOf<host_cpu_load_info>() / sizeOf<IntVar>()).toUInt()
    val ticks = allocArray<IntVar>(4)
    val kr = host_statistics(mach_host_self(), HOST_CPU_LOAD_INFO, ticks.reinterpret(), count.ptr)
    if (kr != 0) return@memScoped null
    val user = ticks[0]
    val system = ticks[1]
    val idle = ticks[2]
    val nice = ticks[3]
    CpuTimes(idle = idle.toLong(), total = (user + system + idle + nice).toLong())
}

@OptIn(ExperimentalForeignApi::class)
internal fun fileSystemFreeSize(path: String = "/"): Long {
    val attrs = NSFileManager.defaultManager.attributesOfFileSystemForPath(path, null) ?: return 0L
    return (attrs[NSFileSystemFreeSize] as? NSNumber)?.longValue ?: 0L
}

@OptIn(ExperimentalForeignApi::class)
internal fun fileSystemTotalSize(path: String = "/"): Long {
    val attrs = NSFileManager.defaultManager.attributesOfFileSystemForPath(path, null) ?: return 0L
    return (attrs[platform.Foundation.NSFileSystemSize] as? NSNumber)?.longValue ?: 0L
}

@OptIn(ExperimentalForeignApi::class)
internal fun utsMachine(): String = memScoped {
    val u = alloc<utsname>()
    if (uname(u.ptr) != 0) return@memScoped ""
    u.machine.toKString()
}

@OptIn(ExperimentalForeignApi::class)
internal fun utsRelease(): String = memScoped {
    val u = alloc<utsname>()
    if (uname(u.ptr) != 0) return@memScoped ""
    u.release.toKString()
}
