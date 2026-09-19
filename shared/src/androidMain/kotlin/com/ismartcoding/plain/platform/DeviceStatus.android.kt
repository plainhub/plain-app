package com.ismartcoding.plain.platform

import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import com.ismartcoding.plain.activityManager
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.data.DDeviceStatus
import com.ismartcoding.plain.data.DTemperature
import java.io.File
import kotlinx.coroutines.delay

actual suspend fun getDeviceStatus(): DDeviceStatus = DDeviceStatus().apply {
    uptimeSec = SystemClock.elapsedRealtime() / 1000L

    // Sticky ACTION_BATTERY_CHANGED intent: one broadcast query, no receiver.
    val intent = appContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    if (intent != null) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level >= 0 && scale > 0) {
            batteryLevel = (level * 100 / scale).coerceIn(0, 100)
        }
        charging = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_CHARGING
        // EXTRA_TEMPERATURE is tenths of a degree Celsius; MIN_VALUE = absent.
        val tenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
        if (tenths != Int.MIN_VALUE) {
            temperatures = listOf(
                DTemperature().apply {
                    label = "battery"
                    celsius = tenths / 10.0
                },
            )
        }
    }

    cpuUsage = readCpuUsage()

    val mi = android.app.ActivityManager.MemoryInfo()
    activityManager.getMemoryInfo(mi)
    memoryAvailable = mi.availMem

    storageAvailable = StatFs(Environment.getDataDirectory().path).availableBytes
}

private suspend fun readCpuUsage(): Double {
    val prev = readProcStat() ?: return 0.0
    delay(CPU_SAMPLE_INTERVAL_MS)
    val cur = readProcStat() ?: return 0.0
    return cpuUsagePercent(prev, cur)
}

internal fun readProcStat(): CpuTimes? {
    val line = runCatching { File("/proc/stat").bufferedReader().readLine() }.getOrNull() ?: return null
    return parseProcStatCpuTimes(line)
}
