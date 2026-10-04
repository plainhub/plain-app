package com.ismartcoding.plain.platform

import com.ismartcoding.plain.preferences.*

import com.ismartcoding.plain.discover.ensureMdnsInterfacesInstalled
import com.ismartcoding.plain.lib.coIO
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.lib.mdns.MdnsHostResponder
import com.ismartcoding.plain.lib.toByteArray
import com.ismartcoding.plain.discover.RustDiscoveryAdvertisement
import com.ismartcoding.plain.httpserver.HttpServerManager
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create

// OS hooks for the Rust HTTP server and user certificate import.

actual fun getSSLSignature(password: String): ByteArray = RustTlsCertificate.signature()

actual fun generateSSLKeyStore(password: String) {
    RustTlsCertificate.regenerate()
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
            RustTlsCertificate.importPem(provider.certificatePem(), provider.privateKeyPem())
        }
        SslCertImportMode.PEM -> {
            val certPem = readFileText(firstUri)
            val keyPem = readFileText(secondUri)
            RustTlsCertificate.importPem(certPem, keyPem)
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

actual suspend fun startHttpEngineAsync(): Boolean {
    IosPlatformRegistry.httpServerBridge()?.stop()
    return com.ismartcoding.plain.httpserver.RustHttpEngine.start()
}

actual suspend fun stopHttpEngineAsync() = com.ismartcoding.plain.httpserver.RustHttpEngine.stop()

/** No platform side effects on iOS once the server is healthy. */
actual suspend fun onHttpServerStarted() {
    ensureMdnsInterfacesInstalled()
    val service = RustDiscoveryAdvertisement.mdns()
    MdnsHostResponder.start(service.targetHostname, service)
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


actual fun isHttpServerRunning(): Boolean =
    com.ismartcoding.plain.httpserver.RustHttpEngine.isRunning

actual fun isMdnsRunning(): Boolean = MdnsHostResponder.isRunning

actual fun getAwareAttachStatus(): String = "not available"
actual fun getAwareDiscoveredPeerCount(): Int = 0





