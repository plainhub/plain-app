package com.ismartcoding.plain.platform

import com.ismartcoding.plain.preferences.*

import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.chat.peer.PeerStatusManager
import com.ismartcoding.plain.chat.peer.transport.WifiAwareTransport
import com.ismartcoding.plain.enums.HttpServerState
import com.ismartcoding.plain.features.ClipboardWatcher
import com.ismartcoding.plain.features.sms.SmsProviderObserver
import com.ismartcoding.plain.features.sms.SmsHelper
import com.ismartcoding.plain.lib.coIO
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.mdns.NsdHelper
import com.ismartcoding.plain.services.HttpServerService
import com.ismartcoding.plain.services.PNotificationListenerService
import com.ismartcoding.plain.platform.HttpServerManager
import com.ismartcoding.plain.discover.RustMdnsRuntime
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

actual suspend fun getSSLSignature(password: String): ByteArray = RustTlsCertificate.signature()

actual suspend fun generateSSLKeyStore(password: String) {
    RustTlsCertificate.regenerate()
}

actual suspend fun replaceSSLKeyStoreAsync(
    mode: SslCertImportMode,
    firstUri: String,
    secondUri: String,
    password: String,
): ByteArray = withIO {
    when (mode) {
        SslCertImportMode.PKCS12 -> RustTlsCertificate.importPkcs12(readUriBytes(firstUri), password)
        SslCertImportMode.PEM -> RustTlsCertificate.importPem(readUriText(firstUri), readUriText(secondUri))
    }
}

private fun readUriBytes(uriStr: String): ByteArray {
    val uri = Uri.parse(uriStr)
    return appContext.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        ?: throw IllegalStateException("Failed to read the selected file")
}

private fun readUriText(uriStr: String): String = readUriBytes(uriStr).toString(Charsets.UTF_8)

actual suspend fun startHttpEngineAsync(): Boolean =
    com.ismartcoding.plain.platform.RustHttpEngine.start()

actual suspend fun stopHttpEngineAsync(): Unit = withIO {
    com.ismartcoding.plain.platform.RustHttpEngine.stop()
}

// The SMS/MMS hooks below serve the web desktop bridge, whose only clients are
// connected web sessions — so they live exactly as long as the server:
// - SmsProviderObserver pushes a debounced WS event when the phone's SMS
//   database changes, so connected desktops refresh;
// - SmsHelper.restoreSmsSendTracking re-arms the in-memory timeout/cleanup
//   jobs for sends that were in flight before this (re)start; their terminal
//   receipts are persisted in SmsSendResultTracker and replayed to a browser
//   on reconnect (see onWebSocketSessionStarted).
actual suspend fun onHttpServerStarted() {
    val service = HttpServerService.instance ?: return
    NsdHelper.registerServices()
    PNotificationListenerService.toggle(service, Permission.NOTIFICATION_LISTENER.isEnabledAsync())
    SmsProviderObserver.start(service)
    ClipboardWatcher.start()
    SmsHelper.restoreSmsSendTracking()
    PeerStatusManager.start()
}

actual suspend fun onWebSocketSessionStarted() {
    SmsHelper.replayTerminalSmsSendResults()
}

actual suspend fun onHttpServerStopped() {
    RustMdnsRuntime.control("unpublish")
    PeerStatusManager.stop()
    SmsProviderObserver.stop()
    ClipboardWatcher.stop()
    SmsHelper.stopSmsSendTracking()
    HttpServerService.instance?.let { PNotificationListenerService.toggle(it, false) }
}

/**
 * Android entry: start the foreground service, which runs the shared
 * [startHttpServerAsync] orchestrator from its lifecycle coroutine. Retried a
 * few times in case the service can't be started immediately. If every retry
 * fails (e.g. an OEM background restriction hit while the app had just gone
 * to background), the STARTING state recorded by the dispatcher has no writer
 * left — record ERROR so the UI shows the failure instead of spinning forever.
 */
actual fun startHttpServerService() {
    coIO {
        var retry = 3
        var lastError: Exception? = null
        val context = appContext
        while (retry > 0) {
            try {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, HttpServerService::class.java),
                )
                return@coIO
            } catch (ex: Exception) {
                lastError = ex
                LogCat.e(ex.toString())
                delay(500.milliseconds)
                retry--
            }
        }
        HttpServerManager.httpServerError.value = "startForegroundService failed: ${lastError?.message}"
        HttpServerManager.serverState.value = HttpServerState.ERROR
    }
}

/**
 * Android external stop: run the shared stop body, then tear down the
 * foreground service. The service's own lifecycle stop calls
 * [stopHttpServerCoreAsync] directly (without stopping itself again).
 * Beyond cancellation: a stop whose caller's scope dies midway (ViewModel
 * cleared, QS tile destroyed) must not leave the service alive with the
 * engine already stopped.
 */
actual suspend fun stopHttpServiceAsync(): Unit = withIO {
    withContext(NonCancellable) {
        stopHttpServerCoreAsync()
        appContext.stopService(Intent(appContext, HttpServerService::class.java))
    }
}

actual fun isHttpServerRunning(): Boolean = HttpServerService.isRunning()

actual fun isMdnsRunning(): Boolean = RustMdnsRuntime.running

actual fun getAwareAttachStatus(): String =
    if (WifiAwareTransport.awareSession != null) "attached" else "not attached"

actual fun getAwareDiscoveredPeerCount(): Int = WifiAwareTransport.discoveredPeerCount
