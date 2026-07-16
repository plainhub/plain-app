package com.ismartcoding.plain.services
import com.ismartcoding.plain.appContext

import com.ismartcoding.plain.i18n.*

import com.ismartcoding.plain.lib.channel.sendEvent
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.helpers.PortHelper
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.chat.peer.PeerStatusManager
import com.ismartcoding.plain.enums.HttpServerState
import com.ismartcoding.plain.events.HttpServerStateChangedEvent
import com.ismartcoding.plain.platform.Permission
import com.ismartcoding.plain.platform.isEnabledAsync
import com.ismartcoding.plain.platform.LocaleHelper
import com.ismartcoding.plain.web.HttpServerManager
import com.ismartcoding.plain.webserver.checkServerHealthAsync
import com.ismartcoding.plain.webserver.createHttpServerAsync
import com.ismartcoding.plain.webserver.httpServer
import com.ismartcoding.plain.webserver.stopPreviousHttpServer
import com.ismartcoding.plain.mdns.NsdHelper
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.netty.NettyApplicationEngine

/**
 * Handles the HTTP server start sequence with retry logic and port conflict handling.
 */
object HttpServerStartHelper {

    suspend fun startServer(service: HttpServerService, onStateChanged: (HttpServerState) -> Unit) = withIO {
        LogCat.d("startHttpServer")
        onStateChanged(HttpServerState.STARTING)
        sendEvent(HttpServerStateChangedEvent(HttpServerState.STARTING))

        HttpServerManager.portsInUse.clear()
        HttpServerManager.httpServerError = ""

        stopPreviousHttpServer()
        if (PortHelper.isPortInUse(TempData.httpPort.value) || PortHelper.isPortInUse(TempData.httpsPort.value)) {
            LogCat.d("Ports still in use after stopping previous server, waiting...")
            HttpServerManager.waitForPortsAvailable(TempData.httpPort.value, TempData.httpsPort.value)
            attemptServerStart(1)
        } else {
            attemptServerStart(2)
        }

        if (httpServer != null) {
            PeerStatusManager.start()
        }
        val serverUp = checkServerHealthAsync()
        if (serverUp) {
            handleSuccess(service, onStateChanged)
        } else {
            handleFailure(service, onStateChanged)
        }
    }

    private suspend fun attemptServerStart(maxRetries: Int) = withIO {
        for (attempt in 1..maxRetries) {
            var newServer: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? = null
            try {
                newServer = createHttpServerAsync(appContext)
                newServer.start(wait = false)
                httpServer = newServer
                break
            } catch (ex: Exception) {
                // The engine may have partially started (thread pools created) before
                // throwing — always stop it to prevent thread/memory leaks on each failed attempt.
                try { newServer?.stop(0, 0) } catch (_: Exception) {}
                LogCat.e("Server start attempt $attempt/$maxRetries failed: ${ex.message}")
                if (ex is java.net.BindException || ex.cause is java.net.BindException) {
                    if (attempt < maxRetries) {
                        stopPreviousHttpServer()
                        HttpServerManager.waitForPortsAvailable(
                            TempData.httpPort.value, TempData.httpsPort.value, maxWaitMs = 3000,
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
        NsdHelper.registerServices(service, httpPort = TempData.httpPort.value, httpsPort = TempData.httpsPort.value)
        onStateChanged(HttpServerState.ON)
        sendEvent(HttpServerStateChangedEvent(HttpServerState.ON))
        PNotificationListenerService.toggle(service, Permission.NOTIFICATION_LISTENER.isEnabledAsync())
    }

    private fun handleFailure(
        service: HttpServerService, onStateChanged: (HttpServerState) -> Unit,
    ) {
        val serverWasRunning = httpServer != null

        // Stop the server before checking ports — otherwise our own running
        // server is detected as the "occupier", causing a false positive on
        // every restart (common on rooted ROMs with firewall apps like AFWall+).
        stopPreviousHttpServer()

        if (!serverWasRunning) {
            // Server never started — check if ports are occupied by another process.
            if (PortHelper.isPortInUse(TempData.httpPort.value)) HttpServerManager.portsInUse.add(TempData.httpPort.value)
            if (PortHelper.isPortInUse(TempData.httpsPort.value)) HttpServerManager.portsInUse.add(TempData.httpsPort.value)
        }
        HttpServerManager.httpServerError = when {
            HttpServerManager.portsInUse.isNotEmpty() -> LocaleHelper.getStringF(
                if (HttpServerManager.portsInUse.size > 1) Res.string.http_port_conflict_errors
                else Res.string.http_port_conflict_error,
                "port", HttpServerManager.portsInUse.joinToString(", "),
            )
            serverWasRunning -> LocaleHelper.getString(Res.string.http_server_health_check_failed)
            HttpServerManager.httpServerError.isNotEmpty() ->
                LocaleHelper.getString(Res.string.http_server_failed) + " (${HttpServerManager.httpServerError})"
            else -> LocaleHelper.getString(Res.string.http_server_failed)
        }
        onStateChanged(HttpServerState.ERROR)
        sendEvent(HttpServerStateChangedEvent(HttpServerState.ERROR))
        PNotificationListenerService.toggle(service, false)
    }
}
