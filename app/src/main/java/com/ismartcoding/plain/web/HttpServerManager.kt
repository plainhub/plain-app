package com.ismartcoding.plain.web

import android.content.Context
import android.content.Intent
import android.util.Base64
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.lib.helpers.CryptoHelper
import com.ismartcoding.lib.helpers.JksHelper
import com.ismartcoding.lib.helpers.JsonHelper
import com.ismartcoding.lib.helpers.NetworkHelper
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.BuildConfig
import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.api.HttpClientManager
import com.ismartcoding.plain.data.HttpServerCheckResult
import com.ismartcoding.plain.enums.HttpServerState
import com.ismartcoding.plain.preferences.PasswordPreference
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.SessionClientTsUpdate
import com.ismartcoding.plain.events.ConfirmToAcceptLoginEvent
import com.ismartcoding.plain.events.HttpServerStateChangedEvent
import com.ismartcoding.plain.events.ReleaseWakeLockEvent
import com.ismartcoding.plain.helpers.NotificationHelper
import com.ismartcoding.plain.helpers.UrlHelper
import com.ismartcoding.plain.preferences.KeyStorePasswordPreference
import com.ismartcoding.plain.services.HttpServerService
import com.ismartcoding.plain.web.websocket.WebSocketSession
import io.ktor.client.plugins.websocket.ws
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.applicationEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.sslConnector
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.websocket.send
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.datetime.Instant
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream
import java.security.KeyStore
import java.security.cert.X509Certificate
import java.util.Collections
import java.util.Timer
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.set
import kotlin.concurrent.timerTask

object HttpServerManager {
    private const val SSL_KEY_ALIAS = Constants.SSL_NAME
    val tokenCache = mutableMapOf<String, ByteArray>() // cache the session token, format: <client_id>:<token>
    val clientIpCache = mutableMapOf<String, String>() // format: <client_id>:<client_ip>
    val wsSessions = Collections.synchronizedSet<WebSocketSession>(LinkedHashSet())
    val clientRequestTs = mutableMapOf<String, Long>()
    var httpServerError: String = ""
    val portsInUse = mutableSetOf<Int>()
    val httpsPorts = setOf(8043, 8143, 8243, 8343, 8443, 8543, 8643, 8743, 8843, 8943)
    val httpPorts = setOf(8080, 8180, 8280, 8380, 8480, 8580, 8680, 8780, 8880, 8980)

    private const val LOGIN_RATE_LIMIT_WINDOW_MS = 60_000L
    private const val LOGIN_RATE_LIMIT_MAX_ATTEMPTS = 5

    private data class RateLimitWindow(var startMs: Long, var count: Int)

    private val loginAttemptsByKey = ConcurrentHashMap<String, RateLimitWindow>()

    private var clientTsJob: Job? = null

    fun tryAcquireLoginAttempt(key: String, nowMs: Long = System.currentTimeMillis()): Boolean {
        if (key.isBlank()) return true
        val window = loginAttemptsByKey.compute(key) { _, existing ->
            if (existing == null || nowMs - existing.startMs >= LOGIN_RATE_LIMIT_WINDOW_MS) {
                RateLimitWindow(nowMs, 1)
            } else {
                existing.count += 1
                existing
            }
        } ?: return true

        // Opportunistic cleanup of stale entries
        if (loginAttemptsByKey.size > 5_000) {
            val threshold = nowMs - (LOGIN_RATE_LIMIT_WINDOW_MS * 2)
            loginAttemptsByKey.entries.removeIf { it.value.startMs < threshold }
        }

        return window.count <= LOGIN_RATE_LIMIT_MAX_ATTEMPTS
    }

    fun getClientIpForLogin(clientId: String, remoteAddress: String?): String {
        val cached = clientIpCache[clientId]
        if (!cached.isNullOrEmpty()) return cached
        val ip = remoteAddress ?: ""
        if (ip.isNotEmpty()) {
            clientIpCache[clientId] = ip
        }
        return ip
    }

    val notificationId: Int by lazy {
        NotificationHelper.generateId()
    }

    suspend fun resetPasswordAsync(): String {
        val password = CryptoHelper.randomPassword(6)
        PasswordPreference.putAsync(MainApp.instance, password)
        return password
    }

    fun getNotificationContent(): String {
        val ip = TempData.mdnsHostname
        return "http://$ip:${TempData.httpPort}\nhttps://$ip:${TempData.httpsPort}"
    }

    suspend fun stopServiceAsync(context: Context) {
        sendEvent(HttpServerStateChangedEvent(HttpServerState.STOPPING))
        try {
            val client = HttpClientManager.httpClient()
            val r = client.get(UrlHelper.getShutdownUrl())
            if (r.status == HttpStatusCode.Gone) {
                LogCat.d("http server is stopped")
            }
        } catch (ex: Exception) {
            LogCat.e(ex.toString())
            ex.printStackTrace()
        }
        context.stopService(Intent(context, HttpServerService::class.java))
        // Ensure notification listener is disabled when server is stopped explicitly
        com.ismartcoding.plain.services.PNotificationListenerService.toggle(context, false)

        // Web server is off; don't keep holding the http_server wakelock.
        sendEvent(ReleaseWakeLockEvent())
        httpServerError = ""
        portsInUse.clear()
        sendEvent(HttpServerStateChangedEvent(HttpServerState.OFF))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun checkServerAsync(): HttpServerCheckResult {
        var websocket = false
        var http = false
        var retry = 2
        val client = HttpClientManager.httpClient()
        while (retry-- > 0) {
            try {
                client.ws(urlString = UrlHelper.getWsTestUrl()) {
                    val reason = this.closeReason.getCompleted()
                    LogCat.d("closeReason: $reason")
                    if (reason?.message == BuildConfig.APPLICATION_ID) {
                        websocket = true
                    }
                }
                retry = 0
            } catch (ex: Exception) {
                delay(300)
                ex.printStackTrace()
                LogCat.e(ex.toString())
            }
        }

        try {
            val r = client.get(UrlHelper.getHealthCheckUrl())
            http = r.bodyAsText() == BuildConfig.APPLICATION_ID
        } catch (ex: Exception) {
            ex.printStackTrace()
            LogCat.e(ex.toString())
        }

        return HttpServerCheckResult(websocket, http)
    }

    private suspend fun passwordToToken(): ByteArray {
        return hashToToken(CryptoHelper.sha512(PasswordPreference.getAsync(MainApp.instance).toByteArray()))
    }

    fun hashToToken(hash: String): ByteArray {
        return hash.substring(0, 32).toByteArray()
    }

    suspend fun loadTokenCache() {
        tokenCache.clear()
        SessionList.getItemsAsync().forEach {
            tokenCache[it.clientId] = Base64.decode(it.token, Base64.NO_WRAP)
        }
    }

    fun generateSSLKeyStore(file: File, password: String) {
        val keyStore = JksHelper.genJksFile(SSL_KEY_ALIAS, password, Constants.SSL_NAME)
        FileOutputStream(file).use {
            keyStore.store(it, password.toCharArray())
        }
    }

    private fun getSSLKeyStore(context: Context, password: String): KeyStore {
        val file = File(context.filesDir, Constants.KEY_STORE_FILE_NAME)
        if (!file.exists()) {
            generateSSLKeyStore(file, password)
        }

        return KeyStore.getInstance("BKS", "BC").apply {
            try {
                file.inputStream().use {
                    load(it, password.toCharArray())
                }
            } catch (ex: Exception) {
                LogCat.e("Failed to load keystore: ${ex.message}, regenerating...")
                ex.printStackTrace()
                // Delete corrupted file and regenerate
                if (file.exists()) {
                    file.delete()
                }
                generateSSLKeyStore(file, password)
                // Reload the newly generated keystore
                file.inputStream().use {
                    load(it, password.toCharArray())
                }
            }
        }
    }

    suspend fun createHttpServerAsync(context: Context): EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration> {
        val password = KeyStorePasswordPreference.getAsync(context)
        val passwordArray = password.toCharArray()
        val httpPort = TempData.httpPort
        val httpsPort = TempData.httpsPort
        val environment = applicationEnvironment {
            log = LoggerFactory.getLogger("ktor.application")
        }

        return embeddedServer(Netty, environment, configure = {
            runningLimit = 1000
            tcpKeepAlive = true
            connector {
                port = httpPort
            }
            enableHttp2 = false
            sslConnector(
                keyStore = getSSLKeyStore(context, password),
                keyAlias = SSL_KEY_ALIAS,
                keyStorePassword = { passwordArray },
                privateKeyPassword = { passwordArray },
            ) {
                port = httpsPort
            }
            channelPipelineConfig = {
                addLast("cancellationDetector", AbortableRequestHandler())
            }
        }, HttpModule.module)
    }

    fun getSSLSignature(context: Context, password: String): ByteArray {
        val keystore = getSSLKeyStore(context, password)
        val cert = keystore.getCertificate(SSL_KEY_ALIAS) as X509Certificate
        return cert.signature
    }

    fun clientTsInterval() {
        if (clientTsJob?.isActive == true) return

        // When there are active sessions/clients we update frequently, otherwise we back off.
        val activeIntervalMs = 5_000L
        val idleIntervalMs = 60_000L
        val activeWindowMs = 5_000L

        clientTsJob = coIO {
            while (kotlinx.coroutines.currentCoroutineContext().isActive) {
                val now = System.currentTimeMillis()
                val updates =
                    clientRequestTs
                        .filter { it.value + activeWindowMs > now }
                        .map { SessionClientTsUpdate(it.key, Instant.fromEpochMilliseconds(it.value)) }

                if (updates.isNotEmpty()) {
                    runCatching {
                        AppDatabase.instance.sessionDao().updateTs(updates)
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

    suspend fun respondTokenAsync(
        event: ConfirmToAcceptLoginEvent,
        clientIp: String,
    ) {
        val token = CryptoHelper.generateChaCha20Key()
        SessionList.addOrUpdateAsync(event.clientId) {
            val r = event.request
            it.clientIP = clientIp
            it.osName = r.osName
            it.osVersion = r.osVersion
            it.browserName = r.browserName
            it.browserVersion = r.browserVersion
            it.token = token
        }
        HttpServerManager.loadTokenCache()
        event.session.send(
            CryptoHelper.chaCha20Encrypt(
                HttpServerManager.passwordToToken(),
                JsonHelper.jsonEncode(
                    AuthResponse(
                        AuthStatus.COMPLETED,
                        token,
                    ),
                ),
            ),
        )
    }
}
