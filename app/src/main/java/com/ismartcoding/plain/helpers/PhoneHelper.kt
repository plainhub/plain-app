package com.ismartcoding.plain.helpers

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import com.ismartcoding.lib.extensions.capitalize
import com.ismartcoding.lib.isTPlus
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.enums.DeviceType
import com.ismartcoding.plain.uiModeManager

object PhoneHelper {
    fun getDeviceName(context: Context): String {
        var name = ""
        try {
            name = Settings.Secure.getString(context.contentResolver, "bluetooth_name") ?: ""
        } catch (e: Exception) {
            LogCat.e(e.toString())
        }
        if (name.isEmpty()) {
            val manufacturer = Build.MANUFACTURER
            val model = Build.MODEL
            return if (model.startsWith(manufacturer)) {
                model.capitalize()
            } else {
                manufacturer.capitalize() + " " + model
            }
        }
        return name
    }

    fun getBatteryPercentage(context: Context): Int {
        var percentage = 0
        val batteryStatus = getBatteryStatusIntent(context)
        if (batteryStatus != null) {
            val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            percentage = (level / scale.toFloat() * 100).toInt()
        }
        return percentage
    }

    fun getDeviceType(context: Context): DeviceType {
        // Check if the device is a TV (based on UI mode or system feature)
        if (uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION ||
            context.packageManager.hasSystemFeature("android.software.leanback")) {
            return DeviceType.TV
        }

        // Check if the device is a tablet (based on smallest screen width in dp)
        val config = context.resources.configuration
        if (config.smallestScreenWidthDp >= 600) {
            return DeviceType.TABLET
        }

        // Optional: Check if the device resembles a computer (Chromebook, emulator, Android-x86, etc.)
        val model = Build.MODEL.lowercase()
        val product = Build.PRODUCT.lowercase()
        val manufacturer = Build.MANUFACTURER.lowercase()
        val isComputerLike = listOf(model, product, manufacturer).any {
            it.contains("chromebook") || it.contains("pc") || it.contains("desktop") ||
                    it.contains("nox") || it.contains("emulator")
        }
        if (isComputerLike) {
            return DeviceType.COMPUTER
        }

        // Default to phone if no other conditions matched
        return DeviceType.PHONE
    }

    private fun getBatteryStatusIntent(context: Context): Intent? {
        val batFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        if (isTPlus()) {
            return context.registerReceiver(null, batFilter, Context.RECEIVER_NOT_EXPORTED)
        }

        return context.registerReceiver(null, batFilter)
    }
}