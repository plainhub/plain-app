package com.ismartcoding.plain.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.plain.BuildConfig
import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.preferences.WebPreference
import com.ismartcoding.plain.services.ScreenMirrorService
import com.ismartcoding.plain.web.HttpServerManager

class ServiceStopBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        coIO {
            if (intent.action == Constants.ACTION_STOP_HTTP_SERVER) {
                WebPreference.putAsync(context, false)
                HttpServerManager.stopServiceAsync(context)
            } else if (intent.action == Constants.ACTION_STOP_SCREEN_MIRROR) {
                ScreenMirrorService.instance?.stop()
                ScreenMirrorService.instance = null
            }
        }
    }
}
