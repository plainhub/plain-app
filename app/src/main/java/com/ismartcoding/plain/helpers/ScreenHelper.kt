package com.ismartcoding.plain.helpers

import android.annotation.SuppressLint
import android.content.Context
import com.ismartcoding.lib.extensions.getSystemScreenTimeout
import com.ismartcoding.lib.extensions.setSystemScreenTimeout
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.data.preference.KeepScreenOnPreference
import com.ismartcoding.plain.data.preference.SystemScreenTimeoutPreference
import com.ismartcoding.plain.features.Permission

object ScreenHelper {
    @SuppressLint("CheckResult")
    fun keepScreenOn(context: Context, enable: Boolean): Boolean {
        val contentResolver = context.contentResolver
        if (Permission.WRITE_SETTINGS.can(context)) {
            saveOn(enable)
            if (enable) {
                saveTimeout(contentResolver.getSystemScreenTimeout())
                contentResolver.setSystemScreenTimeout(Int.MAX_VALUE)
            } else {
                val systemScreenTimeout = SystemScreenTimeoutPreference.get(context)
                contentResolver.setSystemScreenTimeout(if (systemScreenTimeout > 0) systemScreenTimeout else 5000 * 60) // default 5 minutes
            }
            return true
        } else {
            Permission.WRITE_SETTINGS.grant(context)
        }
        return false
    }

    fun saveTimeout(value: Int) {
        SystemScreenTimeoutPreference.put(MainApp.instance, MainApp.instance.ioScope, value)
    }

    fun saveOn(value: Boolean) {
        KeepScreenOnPreference.put(MainApp.instance, MainApp.instance.ioScope, value)
    }
}