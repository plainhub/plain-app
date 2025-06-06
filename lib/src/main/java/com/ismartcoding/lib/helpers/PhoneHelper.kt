package com.ismartcoding.lib.helpers

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import com.ismartcoding.lib.extensions.capitalize
import com.ismartcoding.lib.isTPlus
import com.ismartcoding.lib.logcat.LogCat

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

    private fun getBatteryStatusIntent(context: Context): Intent? {
        val batFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        if (isTPlus()) {
            return context.registerReceiver(null, batFilter, Context.RECEIVER_NOT_EXPORTED)
        }

        return context.registerReceiver(null, batFilter)
    }
}
