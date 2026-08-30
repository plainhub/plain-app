package com.ismartcoding.plain.platform

import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.chat.peer.PeerStatusManager
import com.ismartcoding.plain.features.sms.SmsProviderObserver
import com.ismartcoding.plain.features.sms.SmsHelper
import com.ismartcoding.plain.lib.coIO
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.mdns.NsdHelper
import com.ismartcoding.plain.preferences.KeyStorePasswordPreference
import com.ismartcoding.plain.services.HttpServerService
import com.ismartcoding.plain.services.PNotificationListenerService
import com.ismartcoding.plain.httpserver.HttpServerManager
import com.ismartcoding.plain.httpserver.createHttpServerAsync
import com.ismartcoding.plain.httpserver.generateSslKeyStoreFile
import com.ismartcoding.plain.httpserver.getSslSignatureBytes
import com.ismartcoding.plain.httpserver.httpServer
import com.ismartcoding.plain.httpserver.replaceSslKeyStoreBytes
import com.ismartcoding.plain.httpserver.replaceSslKeyStoreFromPem
import kotlinx.coroutines.delay
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

actual fun getSSLSignature(password: String): ByteArray =
    getSslSignatureBytes(appContext, password)

actual fun generateSSLKeyStore(password: String) {
    generateSslKeyStoreFile(File(appContext.filesDir, Constants.KEY_STORE_FILE_NAME), password)
}

actual suspend fun replaceSSLKeyStoreAsync(
    mode: SslCertImportMode,
    firstUri: String,
    secondUri: String,
    password: String,
): ByteArray = withIO {
    val file = File(appContext.filesDir, Constants.KEY_STORE_FILE_NAME)
    val keystorePassword = KeyStorePasswordPreference.getAsync()
    when (mode) {
        SslCertImportMode.PKCS12 -> {
            val bytes = readUriBytes(firstUri)
            replaceSslKeyStoreBytes(file, bytes, password, keystorePassword)
        }
        SslCertImportMode.PEM -> {
            val certPem = readUriText(firstUri)
            val keyPem = readUriText(secondUri)
            replaceSslKeyStoreFromPem(file, certPem, keyPem, keystorePassword)
        }
    }
}

private fun readUriBytes(uriStr: String): ByteArray {
    val uri = Uri.parse(uriStr)
    return appContext.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        ?: throw IllegalStateException("Failed to read the selected file")
}

private fun readUriText(uriStr: String): String = readUriBytes(uriStr).toString(Charsets.UTF_8)

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
    SmsProviderObserver.start(service)
    SmsHelper.restoreSmsSendTracking()
    PeerStatusManager.start()
}

actual suspend fun onWebSocketSessionStarted() {
    SmsHelper.replayTerminalSmsSendResults()
    replayTerminalMmsSendResults()
}

actual suspend fun onHttpServerStopped() {
    NsdHelper.unregisterService()
    PeerStatusManager.stop()
    SmsProviderObserver.stop()
    SmsHelper.stopSmsSendTracking()
    cancelMmsPolling()
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
                delay(500.milliseconds)
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
