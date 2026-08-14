package com.ismartcoding.plain.platform

import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.discover.PairingCore
import com.ismartcoding.plain.helpers.coIO
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.mdns.MdnsHostResponder
import com.ismartcoding.plain.mdns.buildMdnsServiceInfo
import com.ismartcoding.plain.httpserver.HttpServerManager

/**
 * iOS implementation of the HTTP server platform contract, backed by the
 * SwiftNIO server living in the iosApp Swift target.
 *
 * Only the lowest-level engine lifecycle (start/stop the SwiftNIO bridge) and
 * the SSL cert provider live here; all business logic (state transitions,
 * retry, health probing, error formatting, event emission) is shared in
 * commonMain's [startHttpServerAsync] / [stopHttpServerCoreAsync].
 */

actual fun getSSLSignature(password: String): ByteArray =
    IosPlatformRegistry.sslCertProvider()?.getCertSignatureBytes() ?: ByteArray(0)

actual fun generateSSLKeyStore(password: String) {
    IosPlatformRegistry.sslCertProvider()?.regenerateCert()
}

/**
 * iOS engine start: drive the Swift `PlainHttpServer` via [IosHttpServerBridge].
 * Returns `true` when the SwiftNIO bootstrap bound the configured ports.
 * Failure reason is recorded in [HttpServerManager.httpServerError] for the
 * common orchestrator to surface.
 */
actual suspend fun startHttpEngineAsync(): Boolean = withIO {
    val bridge = IosPlatformRegistry.httpServerBridge()
    if (bridge == null) {
        HttpServerManager.httpServerError = "iOS HTTP server bridge not registered — Swift PlainHttpServer missing"
        LogCat.e(HttpServerManager.httpServerError)
        return@withIO false
    }
    val httpPort = TempData.httpPort.value
    val httpsPort = TempData.httpsPort.value
    val ok = try {
        bridge.start(httpPort, httpsPort)
    } catch (ex: Exception) {
        HttpServerManager.httpServerError = ex.message ?: "Failed to start HTTP server"
        LogCat.e("startHttpEngineAsync failed: ${ex.message}")
        false
    }
    if (ok) {
        HttpServerManager.httpServerError = ""
    } else if (HttpServerManager.httpServerError.isEmpty()) {
        HttpServerManager.httpServerError = "Failed to start HTTP server"
    }
    ok
}

/** iOS engine stop: stop the SwiftNIO bridge. */
actual suspend fun stopHttpEngineAsync(): Unit = withIO {
    IosPlatformRegistry.httpServerBridge()?.stop()
}

/** No platform side effects on iOS once the server is healthy. */
actual suspend fun onHttpServerStarted() {
    // Start mDNS hostname responder so peers can discover this device via its .local name.
    val httpPort = TempData.httpPort.value
    val httpsPort = TempData.httpsPort.value
    if (httpPort > 0 || httpsPort > 0) {
        val hostname = TempData.mdnsHostname
        val service = buildMdnsServiceInfo(PairingCore.buildDiscoverReply(), hostname)
        MdnsHostResponder.start(hostname, service)
    }
}

/** No platform side effects on iOS when the server stops. */
actual suspend fun onHttpServerStopped() {
    MdnsHostResponder.clearService()
}

/**
 * iOS entry: launch a coroutine running the shared [startHttpServerAsync]
 * orchestrator. There is no foreground-service requirement on iOS, so the
 * engine can be driven directly from a background coroutine.
 */
actual fun startHttpServerService() {
    coIO {
        LogCat.d("startHttpServer (iOS/SwiftNIO)")
        startHttpServerAsync()
    }
}

/** iOS external stop: run the shared stop body (no foreground service to tear down). */
actual suspend fun stopHttpServiceAsync(): Unit = withIO {
    stopHttpServerCoreAsync()
}










