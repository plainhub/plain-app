package com.ismartcoding.plain.services

import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.PortHelper
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.enums.HttpServerState
import com.ismartcoding.plain.events.HttpServerStateChangedEvent
import com.ismartcoding.plain.features.Permission
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.web.HttpServerManager
import com.ismartcoding.plain.mdns.NsdHelper
import kotlinx.coroutines.delay

/**
 * Handles the HTTP server start sequence with retry logic and port conflict handling.
 */
object HttpServerStartHelper {

    suspend fun startServer(service: HttpServerService, onStateChanged: (HttpServerState) -> Unit) {
        LogCat.d("startHttpServer")
        onStateChanged(HttpServerState.STARTING)
        sendEvent(HttpServerStateChangedEvent(HttpServerState.STARTING))

        HttpServerManager.portsInUse.clear()
        HttpServerManager.httpServerError = ""

        HttpServerManager.stopPreviousServer()
        if (PortHelper.isPortInUse(TempData.httpPort) || PortHelper.isPortInUse(TempData.httpsPort)) {
            LogCat.d("Ports still in use after stopping previous server, waiting...")
            HttpServerManager.waitForPortsAvailable(TempData.httpPort, TempData.httpsPort)
            attemptServerStart(1)
        } else {
            attemptServerStart(2)
        }

        delay(500)
        val checkResult = HttpServerManager.checkServerAsync()
        if (checkResult.websocket && checkResult.http) {
            handleSuccess(service, onStateChanged)
        } else {
            handleFailure(service, checkResult.http, onStateChanged)
        }
    }

    private suspend fun attemptServerStart(maxRetries: Int) {
        for (attempt in 1..maxRetries) {
            try {
                val server = HttpServerManager.createHttpServerAsync(MainApp.instance)
                server.start(wait = false)
                HttpServerManager.server = server
                break
            } catch (ex: Exception) {
                LogCat.e("Server start attempt $attempt/$maxRetries failed: ${ex.message}")
                if (ex is java.net.BindException || ex.cause is java.net.BindException) {
                    if (attempt < maxRetries) {
                        HttpServerManager.stopPreviousServer()
                        HttpServerManager.waitForPortsAvailable(
                            TempData.httpPort, TempData.httpsPort, maxWaitMs = 3000,
                        )
                    }
                } else {
                    break
                }
            }
        }
    }

    private suspend fun handleSuccess(
        service: HttpServerService, onStateChanged: (HttpServerState) -> Unit,
    ) {
        HttpServerManager.httpServerError = ""
        HttpServerManager.portsInUse.clear()
        NsdHelper.registerServices(service, httpPort = TempData.httpPort, httpsPort = TempData.httpsPort)
        onStateChanged(HttpServerState.ON)
        sendEvent(HttpServerStateChangedEvent(HttpServerState.ON))
        PNotificationListenerService.toggle(service, Permission.NOTIFICATION_LISTENER.isEnabledAsync(service))
    }

    private fun handleFailure(
        service: HttpServerService, httpOk: Boolean, onStateChanged: (HttpServerState) -> Unit,
    ) {
        val serverWasRunning = HttpServerManager.server != null

        // Stop the server before checking ports — otherwise our own running
        // server is detected as the "occupier", causing a false positive on
        // every restart (common on rooted ROMs with firewall apps like AFWall+).
        HttpServerManager.stopPreviousServer()

        if (!httpOk && !serverWasRunning) {
            // Server never started — check if ports are occupied by another process.
            if (PortHelper.isPortInUse(TempData.httpPort)) HttpServerManager.portsInUse.add(TempData.httpPort)
            if (PortHelper.isPortInUse(TempData.httpsPort)) HttpServerManager.portsInUse.add(TempData.httpsPort)
        }
        HttpServerManager.httpServerError = when {
            HttpServerManager.portsInUse.isNotEmpty() -> LocaleHelper.getStringF(
                if (HttpServerManager.portsInUse.size > 1) R.string.http_port_conflict_errors
                else R.string.http_port_conflict_error,
                "port", HttpServerManager.portsInUse.joinToString(", "),
            )
            serverWasRunning -> LocaleHelper.getString(R.string.http_server_health_check_failed)
            HttpServerManager.httpServerError.isNotEmpty() ->
                LocaleHelper.getString(R.string.http_server_failed) + " (${HttpServerManager.httpServerError})"
            else -> LocaleHelper.getString(R.string.http_server_failed)
        }
        onStateChanged(HttpServerState.ERROR)
        sendEvent(HttpServerStateChangedEvent(HttpServerState.ERROR))
        PNotificationListenerService.toggle(service, false)
    }
}
