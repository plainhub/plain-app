package com.ismartcoding.plain.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.plain.LocalStorage
import com.ismartcoding.plain.features.HttpServerEnabledEvent
import com.ismartcoding.plain.web.HttpServerService

class HttpServerStopBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.ismartcoding.plain.action.stop_http_server") {
            LocalStorage.webConsoleEnabled = false
            sendEvent(HttpServerEnabledEvent(false))
            HttpServerService.instance?.stop()
            HttpServerService.instance = null
        }
    }
}