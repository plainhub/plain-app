package com.ismartcoding.plain.platform

/**
 * Set of HTTP/HTTPS ports that failed to bind on the current platform.
 * Empty when no port conflicts exist.
 */
expect fun httpServerPortsInUse(): Set<Int>

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
 * Reset the web console password to a new random value and persist it.
 * @return the new password
 */
expect suspend fun resetPasswordAsync(): String

/**
 * Probe the embedded HTTP server health endpoint.
 * @return true if the server responds with HTTP 200
 */
expect suspend fun checkHttpServerAsync(): Boolean

/**
 * Dispose the embedded HTTP server immediately. Called by the `/shutdown` route
 * after the response is flushed. Safe to call when no server is running.
 */
expect suspend fun disposeHttpServer()

/**
 * Stop the embedded HTTP server and the foreground service (on Android), then
 * notify listeners. No-op on platforms without an HTTP server. Equivalent to
 * the Android-only `webserver.stopHttpServiceAsync(context)` but uses the
 * platform's app context internally so it can be called from commonMain.
 */
expect suspend fun stopHttpServiceAsync()

/**
 * Start the embedded HTTP server foreground service (on Android). Retries up
 * to 3 times on failure. No-op on platforms without an HTTP server service.
 * Equivalent to the Android-only `Intent(context, HttpServerService::class.java)`
 * startForegroundService call but uses the platform's app context internally.
 */
expect fun startHttpServerService()
