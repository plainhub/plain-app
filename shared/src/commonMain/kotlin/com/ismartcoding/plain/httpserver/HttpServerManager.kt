package com.ismartcoding.plain.httpserver

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateSetOf
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.db.SessionClientTsUpdate
import com.ismartcoding.plain.events.ConfirmToAcceptLoginEvent
import com.ismartcoding.plain.helpers.Base64Lenient
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.helpers.SignatureHelper
import com.ismartcoding.plain.helpers.TimeHelper
import com.ismartcoding.plain.lib.coIO
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.platform.AppDatabase
import com.ismartcoding.plain.platform.computeECDHSharedKey
import com.ismartcoding.plain.platform.generateECDHKeyPair
import com.ismartcoding.plain.platform.generateNotificationId
import com.ismartcoding.plain.platform.chaCha20Encrypt
import com.ismartcoding.plain.platform.isPortInUse
import com.ismartcoding.plain.platform.randomPassword
import com.ismartcoding.plain.platform.sendWebLoginNotification
import com.ismartcoding.plain.platform.sha512
import com.ismartcoding.plain.preferences.PasswordPreference
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Instant

/**
 * Pure business logic and state for the embedded HTTP/HTTPS server.
 *
 * Ktor/Netty/SSL lifecycle (start/stop the embedded server, manage the JKS
 * keystore, probe the health endpoint) lives in the platform-specific
 * `webserver` package and calls into this object for shared state.
 */
object HttpServerManager {
    // These caches are mutated concurrently from every in-flight HTTP/WebSocket
    // request (each handled on its own Netty/IO thread), so a plain
    // mutableMapOf/mutableSetOf (LinkedHashMap/LinkedHashSet) is not safe here —
    // concurrent puts can corrupt the internal structure and crash the app
    // (observed as repeated-request crashes). Use Compose's SnapshotStateMap/Set
    // for KMP-safe concurrent access (same convention as PeerTransportPrewarmer).

    /** Cached session token keyed by client id. */
    val tokenCache = mutableStateMapOf<String, ByteArray>()

    /** Cached client IP keyed by client id. */
    val clientIpCache = mutableStateMapOf<String, String>()

    /** Active WebSocket sessions. Platform implementations register here on connect. */
    val wsSessions = mutableStateSetOf<WsSessionHandle>()

    /** Last API request timestamp per client id, used to drive session activity updates. */
    val clientRequestTs = mutableStateMapOf<String, Long>()

    /** Last server start error message, empty when the server is healthy. */
    var httpServerError: String = ""

    /** Ports that failed to bind on the last start attempt. */
    val portsInUse = mutableStateSetOf<Int>()

    /** Stable notification id used for the foreground service and server-status notifications. */
    val notificationId: Int by lazy { generateNotificationId() }

    private const val LOGIN_RATE_LIMIT_WINDOW_MS = 60_000L
    private const val LOGIN_RATE_LIMIT_MAX_ATTEMPTS = 5

    private data class RateLimitWindow(var startMs: Long, var count: Int)

    private val loginAttemptsByKey = mutableMapOf<String, RateLimitWindow>()
    private val loginAttemptsMutex = Mutex()

    private var clientTsJob: Job? = null

    /**
     * Attempt to consume a login attempt slot for [key] (either a client IP or
     * `cid:<clientId>`). Returns true when the attempt is allowed, false when the
     * client has exceeded the rate limit within the rolling window.
     */
    suspend fun tryAcquireLoginAttempt(key: String, nowMs: Long = TimeHelper.nowMillis()): Boolean {
        if (key.isBlank()) return true
        return loginAttemptsMutex.withLock {
            val existing = loginAttemptsByKey[key]
            val window = if (existing == null || nowMs - existing.startMs >= LOGIN_RATE_LIMIT_WINDOW_MS) {
                RateLimitWindow(nowMs, 1)
            } else {
                existing.count += 1
                existing
            }
            loginAttemptsByKey[key] = window

            // Opportunistic cleanup of stale entries
            if (loginAttemptsByKey.size > 5_000) {
                val threshold = nowMs - (LOGIN_RATE_LIMIT_WINDOW_MS * 2)
                loginAttemptsByKey.entries.removeAll { it.value.startMs < threshold }
            }

            window.count <= LOGIN_RATE_LIMIT_MAX_ATTEMPTS
        }
    }

    /**
     * Resolve the client IP to attribute a login attempt to. Prefers the cached
     * value (set by `/init`) and falls back to [remoteAddress] from the socket.
     */
    fun getClientIpForLogin(clientId: String, remoteAddress: String): String {
        val cached = clientIpCache[clientId]
        if (!cached.isNullOrEmpty()) return cached
        if (remoteAddress.isNotEmpty()) {
            clientIpCache[clientId] = remoteAddress
        }
        return remoteAddress
    }

    /** Reset the web console password to a new random value and persist it. */
    suspend fun resetPasswordAsync(): String {
        val password = randomPassword(6)
        PasswordPreference.putAsync(password)
        return password
    }

    /** Derive the ChaCha20 token (first 32 bytes of the SHA-512 of the password). */
    suspend fun passwordToToken(): ByteArray {
        val password = PasswordPreference.getAsync()
        return withIO { hashToToken(sha512(password.encodeToByteArray())) }
    }

    /** Truncate a SHA-512 hex hash to the 32-byte ChaCha20 key. */
    fun hashToToken(hash: String): ByteArray {
        return hash.substring(0, 32).encodeToByteArray()
    }

    /** Reload [tokenCache] from the persisted sessions. */
    suspend fun loadTokenCache() = withIO {
        tokenCache.clear()
        SessionList.getItemsAsync().forEach {
            if (it.token.isNotEmpty()) {
                tokenCache[it.clientId] = Base64Lenient.decode(it.token)
            }
        }
    }

    /**
     * Body of the foreground-service notification: the http and https URLs the
     * client should open.
     */
    fun getNotificationContent(): String {
        val ip = TempData.mdnsHostname
        return "http://$ip:${TempData.httpPort.value}\nhttps://$ip:${TempData.httpsPort.value}"
    }

    /**
     * Persist the session activity timestamps back to the database on a cadence
     * that adapts to whether there is recent traffic. Safe to call multiple
     * times; only the first call starts the loop.
     */
    fun clientTsInterval() {
        if (clientTsJob?.isActive == true) return

        // When there are active sessions/clients we update frequently, otherwise we back off.
        val activeIntervalMs = 5_000L
        val idleIntervalMs = 60_000L
        var lastSyncTs = 0L
        clientTsJob = coIO {
            while (currentCoroutineContext().isActive) {
                val updates =
                    clientRequestTs
                        .filter { it.value > lastSyncTs }
                        .map { SessionClientTsUpdate(it.key, Instant.fromEpochMilliseconds(it.value)) }
                if (updates.isNotEmpty()) {
                    val maxTsInThisBatch = updates.maxOf { it.lastActiveAt.toEpochMilliseconds() }
                    runCatching {
                        AppDatabase.instance.sessionDao().updateTs(updates)
                        lastSyncTs = maxTsInThisBatch
                    }.onFailure {
                        LogCat.e("Failed to update client session timestamps: ${it.message}")
                    }
                    delay(activeIntervalMs)
                } else {
                    delay(idleIntervalMs)
                }
            }
        }
    }

    /**
     * Wait for [httpPort] and [httpsPort] to become free after stopping a
     * previous server. Returns true when both ports are free within [maxWaitMs].
     */
    suspend fun waitForPortsAvailable(httpPort: Int, httpsPort: Int, maxWaitMs: Long = 3000): Boolean = withIO {
        val interval = 200L
        var elapsed = 0L
        while (elapsed < maxWaitMs) {
            val httpFree = !isPortInUse(httpPort)
            val httpsFree = !isPortInUse(httpsPort)
            if (httpFree && httpsFree) {
                LogCat.d("Ports $httpPort and $httpsPort are free after ${elapsed}ms")
                return@withIO true
            }
            delay(interval)
            elapsed += interval
        }
        LogCat.e("Ports still in use after ${maxWaitMs}ms - http:${isPortInUse(httpPort)}, https:${isPortInUse(httpsPort)}")
        false
    }

    /**
     * Accept a pending web login: perform ECDH key exchange to derive a
     * shared token (never transmitted), sign the response with Ed25519,
     * persist the session, refresh the token cache, and send the encrypted
     * response back to the browser via the WebSocket session handle.
     */
    @OptIn(ExperimentalEncodingApi::class)
    suspend fun respondTokenAsync(
        event: ConfirmToAcceptLoginEvent,
        clientIp: String,
    ) = withIO {
        val r = event.request
        val serverKeyPair = generateECDHKeyPair()
        val serverPublicKeyBase64 = Base64.encode(serverKeyPair.publicKeyEncoded)

        val clientPublicKeyBytes = Base64Lenient.decode(event.clientEcdhPublicKey)
        val token = computeECDHSharedKey(serverKeyPair.privateKeyEncoded, clientPublicKeyBytes)
        if (token == null) {
            LogCat.e("ECDH shared key computation failed")
            return@withIO
        }

        val timestamp = TimeHelper.nowMillis()
        val response = AuthResponse(
            clientId = TempData.clientId,
            status = AuthStatus.COMPLETED,
            ecdhPublicKey = serverPublicKeyBase64,
            signature = "", // filled after signing
            timestamp = timestamp,
        )
        val signature = SignatureHelper.signTextAsync(response.toSignatureData())
        val signedResponse = response.copy(signature = signature)

        SessionList.addOrUpdateAsync(event.clientId) {
            it.clientIP = clientIp
            it.osName = r.osName
            it.osVersion = r.osVersion
            it.browserName = r.browserName
            it.browserVersion = r.browserVersion
            it.token = token
        }
        loadTokenCache()
        sendWebLoginNotification(r.browserName, r.browserVersion, r.osName, r.osVersion, clientIp)
        event.session.send(
            chaCha20Encrypt(
                passwordToToken(),
                JsonHelper.jsonEncode(signedResponse),
            ),
        )
    }
}
