package com.ismartcoding.plain.receivers

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.ismartcoding.lib.isTPlus
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.data.DBattery

object BatteryReceiver {
    fun get(context: Context): DBattery {
        val data = DBattery()
        val intent = if (isTPlus()) {
                context.registerReceiver(
                    null,
                    IntentFilter(Intent.ACTION_BATTERY_CHANGED),
                    Context.RECEIVER_NOT_EXPORTED,
                )
            } else {
                context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            }
        if (intent != null) {
            data.level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            data.voltage = intent.getIntExtra("voltage", 0)
            data.temperature = intent.getIntExtra("temperature", 0) / 10
            data.status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            data.plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
            data.health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)
            data.technology = intent.extras?.getString(BatteryManager.EXTRA_TECHNOLOGY) ?: ""
        }
        data.capacity = getBatteryCapacity(context)
        BatteryManager.BATTERY_HEALTH_COLD
        return data
    }

    private fun getBatteryCapacity(context: Context): Int {
        var batteryCapacity = 0.0
        val POWER_PROFILE_CLASS = "com.android.internal.os.PowerProfile"
        try {
            val mPowerProfile = Class.forName(POWER_PROFILE_CLASS).getConstructor(Context::class.java).newInstance(context)
            batteryCapacity = Class.forName(POWER_PROFILE_CLASS).getMethod("getAveragePower", String::class.java).invoke(mPowerProfile, "battery.capacity") as Double
        } catch (ex: Exception) {
            LogCat.e(ex.toString())
            ex.printStackTrace()
        }
        return batteryCapacity.toInt()
    }
}
