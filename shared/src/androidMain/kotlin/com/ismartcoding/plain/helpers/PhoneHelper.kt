package com.ismartcoding.plain.helpers

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.provider.Settings
import com.ismartcoding.plain.lib.extensions.capitalize
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.enums.DeviceType
import com.ismartcoding.plain.uiModeManager

object PhoneHelper {
    fun getDeviceName(context: Context): String {
        var name = Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME) ?: ""
        if (name.isEmpty()) {
            try {
                name = Settings.Secure.getString(context.contentResolver, "bluetooth_name") ?: ""
            } catch (e: Exception) {
                LogCat.e(e.toString())
            }
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

}