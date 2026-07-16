package com.ismartcoding.plain.platform

import com.ismartcoding.plain.data.DBattery

/**
 * Battery level as a percentage (0-100), or -1 if unknown / battery-less device.
 */
expect fun getBatteryLevel(): Int

/**
 * Returns the full battery state (level, voltage, temperature, etc.).
 * On platforms without a battery API, returns a default DBattery with level=-1.
 */
expect fun getBattery(): DBattery

/**
 * Whether the device is currently connected to USB power.
 * False on platforms without USB power detection.
 */
expect fun isUsbConnected(): Boolean