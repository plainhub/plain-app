package com.ismartcoding.plain.helpers

import android.content.Context
import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.plain.preferences.KeepScreenOnPreference

object ScreenHelper {
    suspend fun keepScreenOnAsync(
        context: Context,
        enable: Boolean,
    ) {
        saveOn(context, enable)
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
