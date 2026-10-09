package com.ismartcoding.plain.platform

import com.ismartcoding.plain.api.RustContentApi
import com.ismartcoding.plain.enums.HttpServerState
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.http_server_failed
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.lib.coIO
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.lib.logcat.LogCat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonPrimitive

expect suspend fun getSSLSignature(password: String): ByteArray

expect suspend fun generateSSLKeyStore(password: String)

/**
 * Source format of a user-provided SSL certificate for [replaceSSLKeyStoreAsync].
 */
enum class SslCertImportMode {
    /** Single PKCS#12 bundle (.p12/.pfx) + its password. */
    PKCS12,

    /** Two separate PEM files: certificate (CRT/PEM) + private key (KEY/PEM). */
    PEM,
}

/**
 * Replace the HTTPS keystore with a user-provided certificate.
 *
 * @param mode import format; see [SslCertImportMode]
 * @param firstUri URI of the picked file. For [SslCertImportMode.PKCS12] this is
 *        the PKCS#12 bundle; for [SslCertImportMode.PEM] it is the certificate file.
 * @param secondUri URI of the picked private key file (PEM mode only; ignored otherwise).
 * @param password password of the PKCS#12 bundle (PKCS12 mode only; ignored otherwise).
 * @return the raw signature bytes of the newly installed certificate
 * @throws Exception when the file cannot be read or parsed, the password is wrong,
 *         or no usable private key is found
 */
expect suspend fun replaceSSLKeyStoreAsync(
    mode: SslCertImportMode,
    firstUri: String,
    secondUri: String = "",
    password: String = "",
): ByteArray

/**
 * Reset the web console password to a new random value and persist it.
 * @return the new password
 */
suspend fun resetPasswordAsync(): String = com.ismartcoding.plain.features.session.RustWebLogin.resetPassword()

suspend fun startHttpEngineAsync() = RustHttpEngine.start()
suspend fun stopHttpEngineAsync() = RustHttpEngine.stop()

expect suspend fun onHttpServerStarted()
expect suspend fun onWebSocketSessionStarted()
expect suspend fun onHttpServerStopped()
expect fun startHttpServerService()
expect suspend fun stopHttpServiceAsync()

suspend fun checkHttpServerAsync(): Boolean = withIO {
    try {
        val request = JsonHelper.jsonDecode<JsonObject>(JsonHelper.jsonEncode(emptyMap<String, String>()))
        RustContentApi.postJsonOrThrow("system/http-server/health", request)
            .getValue("healthy").jsonPrimitive.boolean
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        LogCat.e("Rust HTTP server diagnostic failed: ${error.message}")
        false
    }
}

private val lifecycleMutex = Mutex()
private const val STOP_HOOK_TIMEOUT_MS = 2_000L

suspend fun startHttpServerAsync() = withIO {
    lifecycleMutex.withLock {
        HttpServerManager.serverState.value = HttpServerState.STARTING
        HttpServerManager.httpServerError.value = ""
        try {
            RustHttpEngine.start()
            onHttpServerStarted()
            HttpServerManager.serverState.value = HttpServerState.ON
        } catch (error: Throwable) {
            withContext(NonCancellable) {
                stopEngineAndHooks()
                if (error is CancellationException) {
                    HttpServerManager.serverState.value = HttpServerState.OFF
                } else {
                    recordServerFailure(error.message ?: error::class.simpleName.orEmpty())
                }
            }
            if (error is CancellationException) throw error
        }
    }
}

private suspend fun stopHooks() {
    runCatching { withTimeout(STOP_HOOK_TIMEOUT_MS) { onHttpServerStopped() } }
        .onFailure { LogCat.e("HTTP server stop hook failed: ${it.message}") }
}

private suspend fun stopEngineAndHooks() {
    runCatching { RustHttpEngine.stop() }
        .onFailure { LogCat.e("HTTP server engine stop failed: ${it.message}") }
    stopHooks()
}

private suspend fun recordServerFailure(detail: String) {
    LogCat.e("HTTP server failed: $detail")
    val message = runCatching { LocaleHelper.getStringAsync(Res.string.http_server_failed) }
        .getOrDefault("HTTP server failed")
    HttpServerManager.httpServerError.value = if (detail.isEmpty()) message else "$message ($detail)"
    HttpServerManager.serverState.value = HttpServerState.ERROR
}

internal suspend fun onRustHttpServerFailed(generation: Long, message: String) = withIO {
    withContext(NonCancellable) {
        lifecycleMutex.withLock {
            if (RustHttpEngine.failed(generation)) {
                stopHooks()
                recordServerFailure(message)
            }
        }
    }
}

internal suspend fun finishHttpServerStopAsync() = withIO {
    withContext(NonCancellable) {
        lifecycleMutex.withLock {
            stopEngineAndHooks()
            HttpServerManager.httpServerError.value = ""
            HttpServerManager.serverState.value = HttpServerState.OFF
        }
    }
}

suspend fun stopHttpServerCoreAsync() = withIO {
    withContext(NonCancellable) {
        HttpServerManager.serverState.value = HttpServerState.STOPPING
        finishHttpServerStopAsync()
    }
}

fun restartServer() {
    coIO {
        stopHttpServiceAsync()
        startHttpServerService()
    }
}

expect fun isHttpServerRunning(): Boolean
expect fun isMdnsRunning(): Boolean
expect fun getAwareAttachStatus(): String
expect fun getAwareDiscoveredPeerCount(): Int
