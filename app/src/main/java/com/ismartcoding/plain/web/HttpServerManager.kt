package com.ismartcoding.plain.web

import android.content.Context
import android.util.Base64
import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO
import com.ismartcoding.lib.helpers.CryptoHelper
import com.ismartcoding.lib.helpers.JksHelper
import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.LocalStorage
import com.ismartcoding.plain.web.websocket.WebSocketSession
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.datetime.Instant
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream
import java.security.KeyStore
import java.security.cert.X509Certificate
import java.util.*
import kotlin.collections.set

object HttpServerManager {
    private const val SSL_KEY_ALIAS = Constants.SSL_NAME
    var password = CryptoHelper.randomPassword(6)   // the password to login to web console
    var tokenCache = mutableMapOf<String, ByteArray>() // cache the session token, format: <client_id>:<token>
    val clientIpCache = mutableMapOf<String, String>()  // format: <client_id>:<client_ip>
    val wsSessions = Collections.synchronizedSet<WebSocketSession>(LinkedHashSet())
    val clientRequestTs = mutableMapOf<String, Long>()

    fun resetPassword() {
        password = CryptoHelper.randomPassword(6)
    }

    fun passwordToToken(): ByteArray {
        return hashToToken(CryptoHelper.sha512(password.toByteArray()))
    }

    fun hashToToken(hash: String): ByteArray {
        return hash.substring(0, 32).toByteArray()
    }

    fun loadTokenCache() {
        tokenCache.clear()
        SessionList.getItemsAsync().forEach {
            tokenCache[it.clientId] = Base64.decode(it.token, Base64.NO_WRAP)
        }
    }

    private fun getSSLKeyStore(context: Context): KeyStore {
        val file = File(context.filesDir, "keystore.jks")
        if (!file.exists()) {
            val keyStore = JksHelper.genJksFile(SSL_KEY_ALIAS, LocalStorage.clientId, Constants.SSL_NAME)
            val out = FileOutputStream(file)
            keyStore.store(out, null)
            out.close()
        }

        return KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            file.inputStream().use {
                load(it, null)
            }
        }
    }

    fun createHttpServer(context: Context): NettyApplicationEngine {
        val environment = applicationEngineEnvironment {
            log = LoggerFactory.getLogger("ktor.application")
            connector {
                port = 8080
            }
            sslConnector(
                keyStore = getSSLKeyStore(context),
                keyAlias = SSL_KEY_ALIAS,
                keyStorePassword = { LocalStorage.clientId.toCharArray() },
                privateKeyPassword = { LocalStorage.clientId.toCharArray() }) {
                port = 8443
            }
            module(Application::module)
        }
        return embeddedServer(Netty, environment)
    }

    fun getSSLSignature(context: Context): ByteArray {
        val keystore = getSSLKeyStore(context)
        val cert = keystore.getCertificate(SSL_KEY_ALIAS) as X509Certificate
        return cert.signature
    }

    fun clientTsInterval() {
        Timer().scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                val now = System.currentTimeMillis()
                clientRequestTs.forEach { (clientId, ts) ->
                    if (ts + 5000 > now) {
                        coIO {
                            SessionList.addOrUpdateAsync(clientId) {
                                it.updatedAt = Instant.fromEpochMilliseconds(ts)
                            }
                        }
                    }
                }
            }
        }, 0, 5000)
    }
}