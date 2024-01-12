package com.ismartcoding.plain.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.BuildConfig
import com.ismartcoding.plain.api.HttpClientManager
import com.ismartcoding.plain.features.StopHttpServerDoneEvent
import com.ismartcoding.plain.helpers.UrlHelper
import com.ismartcoding.plain.services.HttpServerService
import com.ismartcoding.plain.services.ScreenMirrorService
import com.ismartcoding.plain.web.HttpServerManager
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode

class ServiceStopBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        coIO {
            if (intent.action == "${BuildConfig.APPLICATION_ID}.action.stop_http_server") {
                try {
                    val client = HttpClientManager.httpClient()
                    val r = client.get(UrlHelper.getShutdownUrl())
                    if (r.status == HttpStatusCode.Gone) {
                        LogCat.d("http server is stopped")
                    }
                } catch (ex: Exception) {
                    LogCat.e(ex.toString())
                    ex.printStackTrace()
                }
                HttpServerManager.stoppedByUser = true
                context.stopService(Intent(context, HttpServerService::class.java))
                sendEvent(StopHttpServerDoneEvent())
            } else if (intent.action == "${BuildConfig.APPLICATION_ID}.action.stop_screen_mirror") {
                ScreenMirrorService.instance?.stop()
                ScreenMirrorService.instance = null
            }
        }
    }
}
