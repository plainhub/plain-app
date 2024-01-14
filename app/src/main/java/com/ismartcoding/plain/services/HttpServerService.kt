package com.ismartcoding.plain.services

import android.content.pm.ServiceInfo
import androidx.core.app.ServiceCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleService
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.lib.helpers.PortHelper
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.BuildConfig
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.api.HttpClientManager
import com.ismartcoding.plain.features.StartHttpServerStateEvent
import com.ismartcoding.plain.helpers.NotificationHelper
import com.ismartcoding.plain.helpers.UrlHelper
import com.ismartcoding.plain.web.HttpServerManager
import io.ktor.client.plugins.websocket.ws
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode

class HttpServerService : LifecycleService() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureDefaultChannel()
        val notification =
            NotificationHelper.createServiceNotification(
                this,
                "${BuildConfig.APPLICATION_ID}.action.stop_http_server",
                getString(R.string.api_service_is_running),
            )
        val id = NotificationHelper.generateId()
        ServiceCompat.startForeground(this, id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)

        lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                when (event) {
                    Lifecycle.Event.ON_START -> {
                        coIO {
                            startHttpServerAsync()
                        }
                    }

                    Lifecycle.Event.ON_STOP -> coIO {
                        stopHttpServerAsync()
                    }

                    else -> Unit
                }
            }
        })
    }

    private suspend fun startHttpServerAsync() {
        LogCat.d("startHttpServer")
        try {
            HttpServerManager.portsInUse.clear()
            HttpServerManager.stoppedByUser = false
            HttpServerManager.httpServerError = ""
            HttpServerManager.createHttpServer(MainApp.instance).start(wait = true)
        } catch (ex: Exception) {
            ex.printStackTrace()
            LogCat.e(ex.toString())
            HttpServerManager.httpServerError = ex.toString()

            if (PortHelper.isPortInUse(TempData.httpPort)) {
                HttpServerManager.portsInUse.add(TempData.httpPort)
            }

            if (PortHelper.isPortInUse(TempData.httpsPort)) {
                HttpServerManager.portsInUse.add(TempData.httpsPort)
            }

            if (HttpServerManager.portsInUse.isNotEmpty()) {
                try {
                    val client = HttpClientManager.httpClient()
                    val r = client.get(UrlHelper.getHealthCheckUrl())
                    if (r.status == HttpStatusCode.OK && r.bodyAsText() == BuildConfig.APPLICATION_ID) {
                        LogCat.d("http server is running")
                        HttpServerManager.portsInUse.clear()
                        client.ws(urlString = UrlHelper.getWsTestUrl()) {
                            val reason = this.closeReason.getCompleted()
                            LogCat.d("closeReason: $reason")
                            if (reason?.message == BuildConfig.APPLICATION_ID) {
                                HttpServerManager.httpServerError = ""
                            }
                            sendEvent(StartHttpServerStateEvent())
                        }
                    } else {
                        sendEvent(StartHttpServerStateEvent())
                    }
                } catch (ex2: Exception) {
                    ex2.printStackTrace()
                    LogCat.e(ex2.toString())
                    sendEvent(StartHttpServerStateEvent())
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private suspend fun stopHttpServerAsync() {
        LogCat.d("stopHttpServer")
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
    }
}
