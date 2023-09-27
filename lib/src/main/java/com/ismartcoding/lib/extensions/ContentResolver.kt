package com.ismartcoding.lib.extensions

import android.content.ContentResolver
import android.provider.Settings
import androidx.annotation.CheckResult
import com.ismartcoding.lib.logcat.LogCat

fun ContentResolver.getSystemScreenTimeout(): Int {
    return try {
        Settings.System.getInt(this, Settings.System.SCREEN_OFF_TIMEOUT)
    } catch (e: Settings.SettingNotFoundException) {
        LogCat.e("Error getting screen timeout", e)
        5000 * 60 // default 5 minutes
    }
}

@CheckResult
fun ContentResolver.setSystemScreenTimeout(timeout: Int): Boolean {
    return try {
        Settings.System.putInt(this, Settings.System.SCREEN_OFF_TIMEOUT, timeout)
        true
    } catch (e: SecurityException) {
        LogCat.e("Error writing screen timeout", e)
        false
    }
}
