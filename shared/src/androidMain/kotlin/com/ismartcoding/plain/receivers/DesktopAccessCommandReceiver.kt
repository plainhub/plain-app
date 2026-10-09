package com.ismartcoding.plain.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ismartcoding.plain.AppIntents
import com.ismartcoding.plain.platform.setDesktopAccessEnabled
import com.ismartcoding.plain.lib.coIO
import com.ismartcoding.plain.lib.logcat.LogCat
import kotlinx.coroutines.CancellationException
import com.ismartcoding.plain.preferences.SystemPrefs

class DesktopAccessCommandReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val enabled = when (intent.action) {
            AppIntents.ACTION_ENABLE_DESKTOP_ACCESS -> true
            AppIntents.ACTION_DISABLE_DESKTOP_ACCESS -> false
            else -> return
        }
        val token = SystemPrefs.adbToken.value
        if (token.isEmpty() || intent.getStringExtra("token") != token) return
        val pending = goAsync()
        coIO {
            try {
                setDesktopAccessEnabled(enabled)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                LogCat.e("Desktop access command failed: ${error.message}")
            } finally {
                pending.finish()
            }
        }
    }
}
