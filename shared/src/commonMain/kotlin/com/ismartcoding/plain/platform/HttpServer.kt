package com.ismartcoding.plain.platform

import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.enums.HttpServerState
import com.ismartcoding.plain.lib.TimeHelper
import com.ismartcoding.plain.helpers.UrlHelper
import com.ismartcoding.plain.lib.coIO
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.httpserver.HttpServerManager
import com.ismartcoding.plain.httpserver.closeAllWsSessions
import com.ismartcoding.plain.httpserver.httpPorts
import com.ismartcoding.plain.httpserver.httpsPorts
import com.ismartcoding.plain.preferences.HttpPortPreference
import com.ismartcoding.plain.preferences.HttpsPortPreference
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

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

/** Platform hook invoked after an authenticated WebSocket session is active. */
expect suspend fun onWebSocketSessionStarted()

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
 * Single `/health` probe against the embedded server: HTTP 200 with our own
 * package name in the body.
 */
private suspend fun PlainHttpClient.probeHealthOnce(): Boolean = try {
    get(UrlHelper.getHealthCheckUrl()).use { it.isOk() && it.bodyAsText() == getOwnPackageName() }
} catch (_: Exception) {
    false
}

/**
 * Probe the embedded HTTP server's `/health` endpoint until it answers, with a
 * bounded retry loop. A final probe after the deadline covers a server that
 * came up right as the deadline expired.
 *
 * @return `true` if the server responds with HTTP 200 within [timeoutMs].
 */
suspend fun checkHttpServerAsync(timeoutMs: Long = 8_500): Boolean = withIO {
    createHttpClient().use { client ->
        val deadline = TimeHelper.nowMillis() + timeoutMs
        while (TimeHelper.nowMillis() < deadline) {
            if (client.probeHealthOnce()) return@withIO true
            delay(300)
        }
        client.probeHealthOnce()
    }
}

/**
 * Serializes the whole engine lifecycle (start orchestrator, stop teardown).
 * Stops used to run unserialized next to a mutex-guarded start, so a quick
 * disable→enable could interleave engine stop/start calls and write terminal
 * states out of order (stop's OFF landing after start's ON). With one lock,
 * engine operations and state writes are totally ordered; the pre-lock
 * STARTING/STOPPING markers record intent immediately and the last lock owner
 * decides the final state.
 */
private val lifecycleMutex = Mutex()

/**
 * The configured port when it is free, otherwise the next free port from the
 * standard candidate list (deterministic insertion order). `null` when every
 * candidate is occupied. The predicate is injectable for tests.
 */
internal fun nextFreePort(
    current: Int,
    candidates: Set<Int>,
    isInUse: (Int) -> Boolean = ::isPortInUse,
): Int? {
    if (!isInUse(current)) return current
    return (candidates - current).firstOrNull { !isInUse(it) }
}

/**
 * Shared start orchestration: records state transitions in
 * [HttpServerManager.serverState] (the single source of truth — collectors
 * read the flow, no event copies), clears stale state, stops any previous
 * engine, falls back to free ports when the configured ones are occupied,
 * starts the engine, probes health, and invokes platform side-effect hooks.
 *
 * On Android this is invoked from the foreground service's coroutine; on iOS
 * it is invoked directly by [startHttpServerService].
 */
suspend fun startHttpServerAsync() = withIO {
    lifecycleMutex.withLock {
        try {
            startHttpServerAsyncLocked()
        } catch (ex: kotlinx.coroutines.CancellationException) {
            throw ex
        } catch (ex: Exception) {
            // The orchestrator owns its terminal state: an unexpected failure
            // (engine create crash, keystore fatal, …) must land in ERROR,
            // never strand callers — and the UI loading spinner — in STARTING.
            // The Android service used to catch this; iOS had no backstop.
            LogCat.e("startHttpServerAsync failed unexpectedly: ${ex.message}")
            HttpServerManager.httpServerError.value = ex.message ?: (ex::class.simpleName ?: "error")
            HttpServerManager.serverState.value = HttpServerState.ERROR
        }
    }
}

private suspend fun startHttpServerAsyncLocked() = withIO {
    val t0 = TimeHelper.nowMillis()
    LogCat.d("startHttpServer")
    HttpServerManager.serverState.value = HttpServerState.STARTING
    HttpServerManager.portsInUse.value = emptySet()
    HttpServerManager.httpServerError.value = ""

    // Stop any previous instance. Engine stop closes its listen sockets
    // synchronously, so a port still occupied afterwards is held by a foreign
    // process — waiting for it never helps; fall back to another port instead.
    stopHttpEngineAsync()

    var started = false
    while (!started) {
        val httpPort = nextFreePort(TempData.httpPort.value, httpPorts)
        val httpsPort = nextFreePort(TempData.httpsPort.value, httpsPorts)
        if (httpPort == null || httpsPort == null) {
            HttpServerManager.portsInUse.value =
                listOf(TempData.httpPort.value, TempData.httpsPort.value).filter { isPortInUse(it) }.toSet()
            break
        }
        if (httpPort != TempData.httpPort.value) {
            LogCat.d("HTTP port ${TempData.httpPort.value} in use, falling back to $httpPort")
            HttpPortPreference.putAsync(httpPort)
        }
        if (httpsPort != TempData.httpsPort.value) {
            LogCat.d("HTTPS port ${TempData.httpsPort.value} in use, falling back to $httpsPort")
            HttpsPortPreference.putAsync(httpsPort)
        }
        started = startHttpEngineAsync()
        // A failed bind with both ports free is not a port conflict (keystore,
        // engine error…): retrying other ports cannot fix it.
        if (!started && !isPortInUse(TempData.httpPort.value) && !isPortInUse(TempData.httpsPort.value)) {
            break
        }
    }

    val tHealth = TimeHelper.nowMillis()
    val healthy = started && checkHttpServerAsync()
    LogCat.d("health check took ${TimeHelper.nowMillis() - tHealth}ms: $healthy")
    if (healthy) {
        HttpServerManager.httpServerError.value = ""
        HttpServerManager.portsInUse.value = emptySet()
        val tHooks = TimeHelper.nowMillis()
        onHttpServerStarted()
        LogCat.d("onHttpServerStarted took ${TimeHelper.nowMillis() - tHooks}ms")
        HttpServerManager.serverState.value = HttpServerState.ON
        LogCat.d("HTTP server started on ports ${TempData.httpPort.value}/${TempData.httpsPort.value}, total ${TimeHelper.nowMillis() - t0}ms")
        return@withIO
    }

    // Failure: stop whatever engine may have started. A port conflict that
    // survived the fallback loop above has already been recorded in portsInUse.
    if (started) {
        stopHttpEngineAsync()
    }
    val portsInUse = HttpServerManager.portsInUse.value
    val engineError = HttpServerManager.httpServerError.value
    HttpServerManager.httpServerError.value = when {
        portsInUse.isNotEmpty() -> LocaleHelper.getStringFAsync(
            if (portsInUse.size > 1) Res.string.http_port_conflict_errors
            else Res.string.http_port_conflict_error,
            portsInUse.joinToString(", "),
        )
        started -> LocaleHelper.getStringAsync(Res.string.http_server_health_check_failed)
        engineError.isNotEmpty() ->
            LocaleHelper.getStringAsync(Res.string.http_server_failed) + " ($engineError)"
        else -> LocaleHelper.getStringAsync(Res.string.http_server_failed)
    }
    onHttpServerStopped()
    HttpServerManager.serverState.value = HttpServerState.ERROR
}

/**
 * Engine teardown shared by the stop orchestrator and the `/shutdown` route:
 * stop the engine, run stop side-effect hooks, clear error/port state, and
 * record the terminal OFF state. Safe to call repeatedly (the stop
 * orchestrator's own `/shutdown` GET triggers this once from the route and
 * once directly).
 *
 * Runs under [lifecycleMutex] inside [NonCancellable]: once a stop has begun
 * it must run to completion — a cancelled caller (ViewModel cleared, QS tile
 * service destroyed) must not strand the state in STOPPING with a half-torn
 * engine.
 */
internal suspend fun finishHttpServerStopAsync() = withIO {
    withContext(NonCancellable) {
        lifecycleMutex.withLock {
            val t0 = TimeHelper.nowMillis()
            stopHttpEngineAsync()
            val tEngine = TimeHelper.nowMillis()
            onHttpServerStopped()
            val tHooks = TimeHelper.nowMillis()
            HttpServerManager.httpServerError.value = ""
            HttpServerManager.portsInUse.value = emptySet()
            HttpServerManager.serverState.value = HttpServerState.OFF
            LogCat.d("finishStop: engine=${tEngine - t0}ms hooks=${tHooks - tEngine}ms total=${tHooks - t0}ms")
        }
    }
}

/**
 * Shared stop body: records STOPPING (intent marker, before the lock so the
 * UI reacts immediately), attempts graceful `/shutdown`, then tears the engine
 * down via [finishHttpServerStopAsync] (which serializes on [lifecycleMutex]
 * and records OFF). Called by the platform [stopHttpServiceAsync] actuals and
 * by the Android service's own lifecycle stop. Does NOT stop the Android
 * foreground service.
 *
 * Beyond cancellation for its whole body: a stop whose caller's scope dies
 * mid-way (ViewModel cleared, QS tile destroyed) must not strand the state in
 * STOPPING with a half-torn engine.
 */
suspend fun stopHttpServerCoreAsync() = withIO {
    withContext(NonCancellable) {
        val t0 = TimeHelper.nowMillis()
        HttpServerManager.serverState.value = HttpServerState.STOPPING
        // Close WebSocket sessions directly instead of GETting /shutdown: the
        // old roundtrip ran the teardown inside a route call coroutine, so the
        // engine stop's disposeAndJoin waited on the very coroutine performing
        // the stop and always burned the full 5s shutdown timeout.
        closeAllWsSessions()
        finishHttpServerStopAsync()
        LogCat.d("stopHttpServerCore total ${TimeHelper.nowMillis() - t0}ms")
    }
}

fun restartServer() {
    coIO {
        stopHttpServiceAsync()
        startHttpServerService()
    }
}
