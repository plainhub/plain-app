package com.ismartcoding.plain.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.plain.BuildConfig
import com.ismartcoding.plain.preferences.WebPreference
import com.ismartcoding.plain.services.ScreenMirrorService
import com.ismartcoding.plain.web.HttpServerManager

class ServiceStopBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        coIO {
            if (intent.action == "${BuildConfig.APPLICATION_ID}.action.stop_http_server") {
                WebPreference.putAsync(context, false)
                HttpServerManager.stopServiceAsync(context)
            } else if (intent.action == "${BuildConfig.APPLICATION_ID}.action.stop_screen_mirror") {
                ScreenMirrorService.instance?.stop()
                ScreenMirrorService.instance = null
            }
        }
    }
}
