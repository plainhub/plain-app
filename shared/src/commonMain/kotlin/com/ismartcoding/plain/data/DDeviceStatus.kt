package com.ismartcoding.plain.data

class DTemperature {
    var label: String = ""
    var celsius: Double = 0.0
}

/**
 * Dynamic device runtime state polled via the `deviceStatus` GraphQL query.
 * Static identity/spec lives in [DDeviceInfo]; the two are deliberately
 * separate queries so lightweight consumers (battery badge) don't pay for
 * full info collection.
 */
class DDeviceStatus {
    var uptimeSec: Long = 0L

    /** 0-100, null when the device has no battery or the level is unknown. */
    var batteryLevel: Int? = null

    /** True only while actively charging; full-while-plugged is false. */
    var charging: Boolean = false

    /** Empty when the platform exposes no temperature sources. */
    var temperatures: List<DTemperature> = emptyList()

    /** 0-100 percent, diffed from two CPU counter samples. */
    var cpuUsage: Double = 0.0

    /** OS-level available memory, null when the platform does not expose it (iOS). */
    var memoryAvailable: Long? = null

    /** Available bytes on the primary data volume. */
    var storageAvailable: Long = 0L
}
