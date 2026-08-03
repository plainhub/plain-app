package com.ismartcoding.plain.platform

import android.content.Intent
import androidx.core.content.ContextCompat
import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.chat.peer.PeerStatusManager
import com.ismartcoding.plain.helpers.coIO
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.mdns.NsdHelper
import com.ismartcoding.plain.platform.isEnabledAsync
import com.ismartcoding.plain.services.HttpServerService
import com.ismartcoding.plain.services.PNotificationListenerService
import com.ismartcoding.plain.web.HttpServerManager
import com.ismartcoding.plain.webserver.createHttpServerAsync
import com.ismartcoding.plain.webserver.generateSslKeyStoreFile
import com.ismartcoding.plain.webserver.getSslSignatureBytes
import com.ismartcoding.plain.webserver.httpServer
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

actual fun getSSLSignature(password: String): ByteArray =
    getSslSignatureBytes(appContext, password)

actual fun generateSSLKeyStore(password: String) {
    generateSslKeyStoreFile(File(appContext.filesDir, Constants.KEY_STORE_FILE_NAME), password)
}

/**
 * Android engine start: create the Ktor/Netty embedded server, bind the
 * configured HTTP+HTTPS connectors, and store the running instance. On failure
 * the partially-started engine is stopped and the error reason is recorded in
 * [HttpServerManager.httpServerError] for the common orchestrator to surface.
 */
actual suspend fun startHttpEngineAsync(): Boolean = withIO {
    val newServer = createHttpServerAsync(appContext)
    try {
        newServer.start(wait = false)
        httpServer = newServer
        HttpServerManager.httpServerError = ""
        true
    } catch (ex: Exception) {
        // The engine may have partially started (thread pools created) before
        // throwing — always stop it to prevent thread/memory leaks.
        try { newServer.stop(0, 0) } catch (_: Exception) {}
        HttpServerManager.httpServerError = ex.message ?: ""
        LogCat.e("startHttpEngineAsync failed: ${ex.message}")
        false
    }
}

/** Android engine stop: stop the Ktor/Netty instance if running and clear the reference. */
actual suspend fun stopHttpEngineAsync() = withIO {
    try { httpServer?.stop(0, 1_000) } catch (_: Exception) {}
    httpServer = null
}

actual suspend fun onHttpServerStarted() {
    val service = HttpServerService.instance ?: return
    NsdHelper.registerServices(TempData.httpPort.value, TempData.httpsPort.value)
    PNotificationListenerService.toggle(service, Permission.NOTIFICATION_LISTENER.isEnabledAsync())
    PeerStatusManager.start()
}

actual suspend fun onHttpServerStopped() {
    NsdHelper.unregisterService()
    PeerStatusManager.stop()
    HttpServerService.instance?.let { PNotificationListenerService.toggle(it, false) }
}

/**
 * Android entry: start the foreground service, which runs the shared
 * [startHttpServerAsync] orchestrator from its lifecycle coroutine. Retried a
 * few times in case the service can't be started immediately.
 */
actual fun startHttpServerService() {
    coIO {
        var retry = 3
        val context = appContext
        while (retry > 0) {
            try {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, HttpServerService::class.java),
                )
                break
            } catch (ex: Exception) {
                LogCat.e(ex.toString())
                kotlinx.coroutines.delay(500.milliseconds)
                retry--
            }
        }
    }
}

/**
 * Android external stop: run the shared stop body, then tear down the
 * foreground service. The service's own lifecycle stop calls
 * [stopHttpServerCoreAsync] directly (without stopping itself again).
 */
actual suspend fun stopHttpServiceAsync(): Unit = withIO {
    stopHttpServerCoreAsync()
    appContext.stopService(Intent(appContext, HttpServerService::class.java))
}
