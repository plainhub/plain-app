package com.ismartcoding.plain.platform

import android.bluetooth.BluetoothManager
import android.content.Context
import com.ismartcoding.plain.appContextValue

actual fun isBluetoothEnabled(): Boolean {
    val ctx = appContextValue ?: return false
    val manager = ctx.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager ?: return false
    return try {
        manager.adapter?.isEnabled == true
    } catch (_: SecurityException) {
        false
    }
}

actual fun isBluetoothSupported(): Boolean {
    val ctx = appContextValue ?: return false
    return ctx.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_BLUETOOTH)
}