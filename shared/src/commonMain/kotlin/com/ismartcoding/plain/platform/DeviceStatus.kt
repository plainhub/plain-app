package com.ismartcoding.plain.platform

import com.ismartcoding.plain.data.DDeviceStatus

/**
 * Collects the dynamic device runtime state (uptime, battery, temperatures,
 * CPU usage, available memory/storage). Cheap-enough to poll; CPU usage is
 * diffed from two counter samples taken [CPU_SAMPLE_INTERVAL_MS] apart.
 */
expect suspend fun getDeviceStatus(): DDeviceStatus

const val CPU_SAMPLE_INTERVAL_MS = 200L
