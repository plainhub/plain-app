package com.ismartcoding.plain.helpers

import android.annotation.SuppressLint
import android.content.Context
import com.ismartcoding.lib.extensions.getSystemScreenTimeout
import com.ismartcoding.lib.extensions.setSystemScreenTimeout
import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.plain.preferences.KeepScreenOnPreference
import com.ismartcoding.plain.preferences.SystemScreenTimeoutPreference
import com.ismartcoding.plain.features.Permission

object ScreenHelper {
    @SuppressLint("CheckResult")
    suspend fun keepScreenOnAsync(
        context: Context,
        enable: Boolean,
    ): Boolean {
        val contentResolver = context.contentResolver
        if (Permission.WRITE_SETTINGS.can(context)) {
            saveOn(context, enable)
            if (enable) {
                saveTimeout(context, contentResolver.getSystemScreenTimeout())
                contentResolver.setSystemScreenTimeout(Int.MAX_VALUE)
            } else {
                val systemScreenTimeout = SystemScreenTimeoutPreference.getAsync(context)
                contentResolver.setSystemScreenTimeout(if (systemScreenTimeout > 0) systemScreenTimeout else 5000 * 60) // default 5 minutes
            }
            return true
        } else {
            Permission.WRITE_SETTINGS.grant(context)
        }
        return false
    }

    fun saveTimeout(
        context: Context,
        value: Int,
    ) {
        coIO {
            SystemScreenTimeoutPreference.putAsync(context, value)
        }
    }

    fun saveOn(
        context: Context,
        value: Boolean,
    ) {
        coIO {
            KeepScreenOnPreference.putAsync(context, value)
        }
    }
}
