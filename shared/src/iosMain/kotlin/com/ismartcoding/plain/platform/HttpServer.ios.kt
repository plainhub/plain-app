package com.ismartcoding.plain.platform

import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.discover.PairingCore
import com.ismartcoding.plain.discover.ensureMdnsInterfacesInstalled
import com.ismartcoding.plain.lib.coIO
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.lib.mdns.MdnsHostResponder
import com.ismartcoding.plain.lib.toByteArray
import com.ismartcoding.plain.discover.buildMdnsServiceInfo
import com.ismartcoding.plain.httpserver.HttpServerManager
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create

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

actual suspend fun replaceSSLKeyStoreAsync(
    mode: SslCertImportMode,
    firstUri: String,
    secondUri: String,
    password: String,
): ByteArray = withIO {
    val provider = IosPlatformRegistry.sslCertProvider()
        ?: throw IllegalStateException("SSL certificate provider not available")
    when (mode) {
        SslCertImportMode.PKCS12 -> {
            val data = readFileBytes(firstUri)
            provider.replaceCertWithPkcs12(data, password)
        }
        SslCertImportMode.PEM -> {
            val certPem = readFileText(firstUri)
            val keyPem = readFileText(secondUri)
            provider.replaceCertWithPem(certPem, keyPem)
        }
    }
}

private fun readFileBytes(uriStr: String): ByteArray {
    val url = NSURL.URLWithString(uriStr) ?: throw IllegalStateException("Failed to read the selected file")
    val path = url.path ?: throw IllegalStateException("Failed to read the selected file")
    return NSFileManager.defaultManager.contentsAtPath(path)?.toByteArray()
        ?: throw IllegalStateException("Failed to read the selected file")
}

private fun readFileText(uriStr: String): String {
    val url = NSURL.URLWithString(uriStr) ?: throw IllegalStateException("Failed to read the selected file")
    val path = url.path ?: throw IllegalStateException("Failed to read the selected file")
    val data = NSFileManager.defaultManager.contentsAtPath(path)
        ?: throw IllegalStateException("Failed to read the selected file")
    return NSString.create(data, NSUTF8StringEncoding)?.toString()
        ?: throw IllegalStateException("Failed to read the selected file")
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
    ensureMdnsInterfacesInstalled()
    // Start mDNS hostname responder so peers can discover this device via its .local name.
    val httpPort = TempData.httpPort.value
    val httpsPort = TempData.httpsPort.value
    if (httpPort > 0 || httpsPort > 0) {
        val hostname = TempData.mdnsHostname
        val service = buildMdnsServiceInfo(PairingCore.buildDiscoverReply(), hostname)
        MdnsHostResponder.start(hostname, service)
    }
}

/** iOS has no Android SMS send-result state to replay. */
actual suspend fun onWebSocketSessionStarted() = Unit

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









