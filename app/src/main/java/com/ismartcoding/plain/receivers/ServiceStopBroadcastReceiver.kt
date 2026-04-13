package com.ismartcoding.plain.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.plain.BuildConfig
import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.preferences.AdbTokenPreference
import com.ismartcoding.plain.preferences.WebPreference
import com.ismartcoding.plain.services.HttpServerService
import com.ismartcoding.plain.services.ScreenMirrorService
import com.ismartcoding.plain.web.HttpServerManager

class ServiceStopBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        when (intent.action) {
            Constants.ACTION_START_HTTP_SERVER -> {
                coIO {
                    val storedToken = AdbTokenPreference.getAsync(context)
                    if (intent.getStringExtra("token") != storedToken) return@coIO
                    WebPreference.putAsync(context, true)
                    ContextCompat.startForegroundService(context, Intent(context, HttpServerService::class.java))
                }
            }

            Constants.ACTION_STOP_HTTP_SERVER -> coIO {
                val storedToken = AdbTokenPreference.getAsync(context)
                if (intent.getStringExtra("token") != storedToken) return@coIO
                WebPreference.putAsync(context, false)
                HttpServerManager.stopServiceAsync(context)
            }

            Constants.ACTION_STOP_SCREEN_MIRROR -> {
                ScreenMirrorService.instance?.stop()
                ScreenMirrorService.instance = null
            }
            // Android 14+ allows FGS notifications to be swiped. Re-post via onStartCommand.
            Constants.ACTION_REPOST_HTTP_NOTIFICATION -> {
                if (HttpServerService.isRunning()) {
                    ContextCompat.startForegroundService(context, Intent(context, HttpServerService::class.java))
                }
            }
        }
    }
}
