package com.ismartcoding.plain.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Binder
import androidx.core.content.ContextCompat
import com.ismartcoding.plain.helpers.coIO
import com.ismartcoding.plain.AppIntents
import com.ismartcoding.plain.preferences.AdbTokenPreference
import com.ismartcoding.plain.preferences.DesktopAccessPreference
import com.ismartcoding.plain.platform.stopHttpServiceAsync
import com.ismartcoding.plain.preferences.ServicePreference
import com.ismartcoding.plain.services.HttpServerService
import com.ismartcoding.plain.services.ScreenMirrorService

class ServiceStopBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        when (intent.action) {
            AppIntents.ACTION_START_HTTP_SERVER -> {
                coIO {
                    val storedToken = AdbTokenPreference.getAsync()
                    if (intent.getStringExtra("token") != storedToken) return@coIO
                    ServicePreference.putAsync(true)
                    ContextCompat.startForegroundService(context, Intent(context, HttpServerService::class.java))
                }
            }

            AppIntents.ACTION_STOP_HTTP_SERVER -> coIO {
                val callerUid = Binder.getCallingUid()
                val appUid = context.applicationInfo.uid
                if (callerUid != appUid) {
                    // External caller (ADB, third-party app) — require token
                    val storedToken = AdbTokenPreference.getAsync()
                    if (intent.getStringExtra("token") != storedToken) return@coIO
                }
                ServicePreference.putAsync(false)
                stopHttpServiceAsync()
            }

            AppIntents.ACTION_STOP_SCREEN_MIRROR -> {
                ScreenMirrorService.instance?.stop()
                ScreenMirrorService.instance = null
            }
            // Android 14+ allows FGS notifications to be swiped. Re-post via onStartCommand.
            AppIntents.ACTION_REPOST_HTTP_NOTIFICATION -> {
                if (HttpServerService.isRunning()) {
                    ContextCompat.startForegroundService(context, Intent(context, HttpServerService::class.java))
                }
            }
        }
    }
}
