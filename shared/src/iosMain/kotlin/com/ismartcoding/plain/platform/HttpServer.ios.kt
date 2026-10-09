package com.ismartcoding.plain.platform

import com.ismartcoding.plain.preferences.*

import com.ismartcoding.plain.lib.coIO
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.discover.RustMdnsRuntime
import com.ismartcoding.plain.lib.toByteArray
import com.ismartcoding.plain.platform.HttpServerManager
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create

// OS hooks for the Rust HTTP server and user certificate import.

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
        SslCertImportMode.PKCS12 -> {
            val data = readFileBytes(firstUri)
            RustTlsCertificate.importPkcs12(data, password)
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

/** No platform side effects on iOS once the server is healthy. */
actual suspend fun onHttpServerStarted() {
    RustMdnsRuntime.control("publish")
}

/** iOS has no Android SMS send-result state to replay. */
actual suspend fun onWebSocketSessionStarted() = Unit

/** No platform side effects on iOS when the server stops. */
actual suspend fun onHttpServerStopped() {
    RustMdnsRuntime.control("unpublish")
}

/**
 * iOS entry: launch a coroutine running the shared [startHttpServerAsync]
 * orchestrator. There is no foreground-service requirement on iOS, so the
 * engine can be driven directly from a background coroutine.
 */
actual fun startHttpServerService() {
    coIO {
        LogCat.d("startHttpServer (iOS/Rust)")
        startHttpServerAsync()
    }
}

/** iOS has no foreground service; HTTP lifetime belongs to the app. */
actual suspend fun stopHttpServiceAsync(): Unit = withIO {
    HttpServerManager.backgroundState.value = com.ismartcoding.plain.enums.HttpServerState.OFF
}


actual fun isHttpServerRunning(): Boolean =
    com.ismartcoding.plain.platform.RustHttpEngine.isRunning

actual fun isMdnsRunning(): Boolean = RustMdnsRuntime.running

actual fun getAwareAttachStatus(): String = "not available"
actual fun getAwareDiscoveredPeerCount(): Int = 0
