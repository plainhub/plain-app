package com.ismartcoding.plain.platform

import com.ismartcoding.plain.preferences.*

import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import com.ismartcoding.plain.i18n.background_online_failed
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

actual suspend fun getSSLSignature(): ByteArray = RustTlsCertificate.signature()

actual suspend fun generateSSLKeyStore() {
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

// The SMS/MMS hooks below serve the web desktop bridge, whose only clients are
// connected web sessions — so they live exactly as long as the server:
// - SmsProviderObserver pushes a debounced WS event when the phone's SMS
//   database changes, so connected desktops refresh;
// - SmsHelper.restoreSmsSendTracking re-arms the in-memory timeout/cleanup
//   jobs for sends that were in flight before this (re)start; their terminal
//   receipts are persisted in SmsSendResultTracker and replayed to a browser
//   on reconnect (see onWebSocketSessionStarted).
actual suspend fun onHttpServerStarted() {
    val service = appContext
    HttpServerResources.start()
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
    HttpServerResources.stop()
    PeerStatusManager.stop()
    SmsProviderObserver.stop()
    ClipboardWatcher.stop()
    SmsHelper.stopSmsSendTracking()
    PNotificationListenerService.toggle(appContext, false)
}

/** Starts optional background retention independently of the HTTP server. */
actual fun startHttpServerService() {
    if (!com.ismartcoding.plain.preferences.UserPrefs.service.value) return
    HttpServerManager.backgroundState.value = HttpServerState.STARTING
    HttpServerManager.backgroundError.value = ""
    coIO {
        var retry = 3
        val context = appContext
        while (retry > 0) {
            try {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, HttpServerService::class.java),
                )
                return@coIO
            } catch (ex: Exception) {
                LogCat.e(ex.toString())
                delay(500.milliseconds)
                retry--
            }
        }
        HttpServerManager.backgroundError.value = LocaleHelper.getString(com.ismartcoding.plain.i18n.Res.string.background_online_failed)
        HttpServerManager.backgroundState.value = HttpServerState.ERROR
    }
}

/** Stops background retention while the app-owned HTTP server keeps running. */
actual suspend fun stopHttpServiceAsync(): Unit = withIO {
    withContext(NonCancellable) {
        if (HttpServerService.isRunning()) HttpServerManager.backgroundState.value = HttpServerState.STOPPING
        appContext.stopService(Intent(appContext, HttpServerService::class.java))
        if (!HttpServerService.isRunning()) HttpServerManager.backgroundState.value = HttpServerState.OFF
        HttpServerManager.backgroundError.value = ""
    }
}

actual fun isHttpServerRunning(): Boolean = RustHttpEngine.isRunning

actual fun isMdnsRunning(): Boolean = RustMdnsRuntime.running

actual fun getAwareAttachStatus(): String =
    if (WifiAwareTransport.awareSession != null) "attached" else "not attached"

actual fun getAwareDiscoveredPeerCount(): Int = WifiAwareTransport.discoveredPeerCount
