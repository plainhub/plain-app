package com.ismartcoding.plain.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.ismartcoding.plain.AppIntents
import com.ismartcoding.plain.platform.HttpServerManager
import com.ismartcoding.plain.services.HttpServerService
import com.ismartcoding.plain.services.ScreenMirrorService

class ServiceStopBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        when (intent.action) {
            AppIntents.ACTION_DISABLE_BACKGROUND_MODE -> {
                val pending = goAsync()
                HttpServerManager.setBackgroundEnabled(false, fromUi = false)
                    .invokeOnCompletion { pending.finish() }
            }

            AppIntents.ACTION_STOP_SCREEN_MIRROR -> {
                ScreenMirrorService.instance?.stop()
                ScreenMirrorService.instance = null
            }
            // Android 14+ allows FGS notifications to be swiped. Re-post via onStartCommand.
            AppIntents.ACTION_REPOST_BACKGROUND_NOTIFICATION -> {
                if (HttpServerService.isRunning()) {
                    ContextCompat.startForegroundService(context, Intent(context, HttpServerService::class.java))
                }
            }
        }
    }
}
