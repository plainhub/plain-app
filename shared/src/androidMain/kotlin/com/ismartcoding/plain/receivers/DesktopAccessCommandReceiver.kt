package com.ismartcoding.plain.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ismartcoding.plain.AppIntents
import com.ismartcoding.plain.platform.setDesktopAccessEnabled
import com.ismartcoding.plain.lib.coIO
import com.ismartcoding.plain.lib.logcat.LogCat
import kotlinx.coroutines.CancellationException
import com.ismartcoding.plain.preferences.RustSystemState

class DesktopAccessCommandReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val enabled = when (intent.action) {
            AppIntents.ACTION_ENABLE_DESKTOP_ACCESS -> true
            AppIntents.ACTION_DISABLE_DESKTOP_ACCESS -> false
            else -> return
        }
        val pending = goAsync()
        coIO {
            try {
                if (!RustSystemState.verifyAdbToken(intent.getStringExtra("token").orEmpty())) return@coIO
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
