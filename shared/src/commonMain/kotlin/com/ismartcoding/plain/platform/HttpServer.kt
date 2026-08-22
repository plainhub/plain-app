package com.ismartcoding.plain.platform

import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.api.isOk
import com.ismartcoding.plain.enums.HttpServerState
import com.ismartcoding.plain.events.HttpServerStateChangedEvent
import com.ismartcoding.plain.lib.TimeHelper
import com.ismartcoding.plain.helpers.UrlHelper
import com.ismartcoding.plain.lib.coIO
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.httpserver.HttpServerManager
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Set of HTTP/HTTPS ports that failed to bind on the current platform.
 * Empty when no port conflicts exist.
 */
fun httpServerPortsInUse(): Set<Int> = HttpServerManager.portsInUse.value

/**
 * SSL certificate signature bytes for the current HTTPS keystore.
 * @param password keystore password
 */
expect fun getSSLSignature(password: String): ByteArray

/**
 * Regenerate the SSL keystore file used by the embedded HTTPS server.
 * @param password keystore password
 */
expect fun generateSSLKeyStore(password: String)

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
suspend fun resetPasswordAsync(): String = HttpServerManager.resetPasswordAsync()

// ----------------------------------------------------------------------------------
// Platform-lowest-level engine lifecycle hooks.
//
// Business logic (state transitions, retries, health probing, error formatting,
// event emission) lives in the commonMain functions below; each `actual` only
// drives the native server engine (Ktor/Netty on Android, SwiftNIO on iOS) and
// the Android-only side effects (mDNS, notification-listener, foreground
// service). iOS actuals for the side-effect hooks are no-ops.
// ----------------------------------------------------------------------------------

/**
 * Start the native HTTP/HTTPS server engine and bind the configured ports.
 * @return `true` when the engine is accepting connections, `false` on failure
 *         (the actual records the failure reason in [HttpServerManager.httpServerError])
 */
expect suspend fun startHttpEngineAsync(): Boolean

/**
 * Stop/dispose the native HTTP server engine immediately. Safe to call when no
 * engine is running. Used by the stop orchestrator and the `/shutdown` route.
 */
expect suspend fun stopHttpEngineAsync()

/**
 * Side effects to run once the server is healthy and reachable: register mDNS,
 * start the peer-status manager, and enable the notification listener
 * (Android). No-op on iOS.
 */
expect suspend fun onHttpServerStarted()

/**
 * Side effects to run when the server stops or fails to start: unregister mDNS,
 * stop the peer-status manager, and disable the notification listener
 * (Android). No-op on iOS. Does NOT stop the Android foreground service — that
 * is handled by [stopHttpServiceAsync] so start-failure does not tear down the
 * still-running service.
 */
expect suspend fun onHttpServerStopped()

/**
 * Start the embedded HTTP server. Android starts a foreground service which
 * then runs the shared [startHttpServerAsync] orchestrator; iOS launches a
 * coroutine directly. Retained as `expect` because the Android entry MUST go
 * through `startForegroundService` for platform lifecycle compliance.
 */
expect fun startHttpServerService()

/**
 * Stop the embedded HTTP server from commonMain. Runs the shared
 * [stopHttpServerCoreAsync] body, then on Android additionally stops the
 * foreground service.
 */
expect suspend fun stopHttpServiceAsync()

// ----------------------------------------------------------------------------------
// Shared commonMain business logic.
// ----------------------------------------------------------------------------------

/**
 * Probe the embedded HTTP server's `/health` endpoint with a bounded retry
 * loop. Shared by Android and iOS — previously each platform duplicated this
 * loop (Android `checkServerHealthAsync`, iOS `checkHttpServerAsync`).
 *
 * @return `true` if the server responds with HTTP 200 within the deadline.
 */
suspend fun checkHttpServerAsync(): Boolean = withIO {
    withTimeoutOrNull(9_000) {
        val client = createHttpClient()
        val deadline = TimeHelper.nowMillis() + 8_500L
        var healthy = false
        while (!healthy && TimeHelper.nowMillis() < deadline) {
            try {
                val response = client.get(UrlHelper.getHealthCheckUrl())
                if (response.isOk() && response.bodyAsText() == getOwnPackageName()) {
                    healthy = true
                }
            } catch (ex: Exception) {
                delay(300)
                LogCat.e("HTTP server check failed: ${ex.message}")
            }
        }
        LogCat.d("HTTP server check healthy: $healthy")
        healthy
    } ?: false
}

/**
 * Shared start orchestration: emits state transitions, clears stale state,
 * stops any previous engine, handles port-conflict retries, starts the engine,
 * probes health, and invokes platform side-effect hooks.
 *
 * On Android this is invoked from the foreground service's coroutine; on iOS
 * it is invoked directly by [startHttpServerService]. The optional
 * [onStateChanged] callback mirrors the emitted [HttpServerStateChangedEvent]
 * for callers (the Android service) that need synchronous local state.
 */
suspend fun startHttpServerAsync(onStateChanged: (HttpServerState) -> Unit = {}) = withIO {
    LogCat.d("startHttpServer")
    onStateChanged(HttpServerState.STARTING)
    sendEvent(HttpServerStateChangedEvent(HttpServerState.STARTING))
    HttpServerManager.portsInUse.value = emptySet()
    HttpServerManager.httpServerError = ""

    val httpPort = TempData.httpPort.value
    val httpsPort = TempData.httpsPort.value

    // Stop any previous instance so the ports are free.
    stopHttpEngineAsync()
    val portsWereInUse = isPortInUse(httpPort) || isPortInUse(httpsPort)
    if (portsWereInUse) {
        LogCat.d("Ports still in use after stopping previous server, waiting...")
        HttpServerManager.waitForPortsAvailable(httpPort, httpsPort)
    }
    // If ports were occupied we only get one fresh attempt; otherwise allow a
    // second try to tolerate a transient bind failure.
    val maxRetries = if (portsWereInUse) 1 else 2

    var started = false
    for (attempt in 1..maxRetries) {
        if (startHttpEngineAsync()) {
            started = true
            break
        }
        LogCat.e("Server start attempt $attempt/$maxRetries failed")
        if (attempt < maxRetries) {
            stopHttpEngineAsync()
            HttpServerManager.waitForPortsAvailable(httpPort, httpsPort, maxWaitMs = 3_000)
        }
    }

    val healthy = started && checkHttpServerAsync()
    if (healthy) {
        HttpServerManager.httpServerError = ""
        HttpServerManager.portsInUse.value = emptySet()
        onHttpServerStarted()
        onStateChanged(HttpServerState.ON)
        sendEvent(HttpServerStateChangedEvent(HttpServerState.ON))
        LogCat.d("HTTP server started on port $httpPort")
        return@withIO
    }

    // Failure: stop whatever engine may have started, then re-check ports so we
    // can distinguish a port conflict from a health-check failure.
    if (started) {
        stopHttpEngineAsync()
    } else {
        if (isPortInUse(httpPort)) HttpServerManager.portsInUse.value += httpPort
        if (isPortInUse(httpsPort)) HttpServerManager.portsInUse.value += httpsPort
    }
    val portsInUse = HttpServerManager.portsInUse.value
    HttpServerManager.httpServerError = when {
        portsInUse.isNotEmpty() -> LocaleHelper.getStringFAsync(
            if (portsInUse.size > 1) Res.string.http_port_conflict_errors
            else Res.string.http_port_conflict_error,
            "port", portsInUse.joinToString(", "),
        )
        started -> LocaleHelper.getStringAsync(Res.string.http_server_health_check_failed)
        HttpServerManager.httpServerError.isNotEmpty() ->
            LocaleHelper.getStringAsync(Res.string.http_server_failed) + " (${HttpServerManager.httpServerError})"
        else -> LocaleHelper.getStringAsync(Res.string.http_server_failed)
    }
    onHttpServerStopped()
    onStateChanged(HttpServerState.ERROR)
    sendEvent(HttpServerStateChangedEvent(HttpServerState.ERROR))
}

/**
 * Shared stop body: emits STOPPING, attempts graceful `/shutdown`, stops the
 * engine, runs side-effect hooks, clears state, and emits OFF. Called by the
 * platform [stopHttpServiceAsync] actuals and by the Android service's own
 * lifecycle stop. Does NOT stop the Android foreground service.
 */
suspend fun stopHttpServerCoreAsync() = withIO {
    sendEvent(HttpServerStateChangedEvent(HttpServerState.STOPPING))
    try {
        // Best-effort graceful shutdown via /shutdown endpoint.
        val client = createHttpClient()
        client.get(UrlHelper.getShutdownUrl())
    } catch (_: Exception) {}
    stopHttpEngineAsync()
    onHttpServerStopped()
    HttpServerManager.httpServerError = ""
    HttpServerManager.portsInUse.value = emptySet()
    sendEvent(HttpServerStateChangedEvent(HttpServerState.OFF))
}

fun restartServer() {
    coIO {
        stopHttpServiceAsync()
        startHttpServerService()
    }
}