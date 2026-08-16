package com.ismartcoding.plain.httpserver

import android.content.Context
import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.lib.coIO
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.lib.helpers.JksHelper
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.preferences.KeyStorePasswordPreference
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.applicationEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.sslConnector
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream
import java.security.KeyStore
import java.security.cert.X509Certificate

/**
 * The live Ktor/Netty embedded server instance, or null when the server is stopped.
 * Platform lifecycle code below owns this reference; business state lives in
 * [HttpServerManager] (commonMain).
 */
@Volatile
var httpServer: EmbeddedServer<*, *>? = null

private val SSL_KEY_ALIAS = Constants.SSL_NAME

/**
 * Start a throwaway Netty engine to preload classes/JIT on app launch so the
 * first real server start is fast.
 */
fun warmUpNetty() {
    coIO {
        try {
            val s = embeddedServer(Netty, port = 0) {}
            s.start(wait = false)
            s.stop(0, 0)
            LogCat.d("Netty warm-up complete")
        } catch (_: Exception) {
        }
    }
}

/**
 * Generate a fresh JKS keystore file and atomically replace [file].
 */
fun generateSslKeyStoreFile(file: File, password: String) {
    val keyStore = JksHelper.genJksFile(SSL_KEY_ALIAS, password, Constants.SSL_NAME)
    // Write to a temp file first, then atomically rename to the target.
    // This prevents a partially-written (corrupted) keystore if the process
    // is killed mid-write (OOM, force-stop, reboot, etc.).
    val tmp = File(file.parent, "${file.name}.tmp")
    try {
        FileOutputStream(tmp).use {
            keyStore.store(it, password.toCharArray())
        }
        tmp.renameTo(file)
    } catch (ex: Exception) {
        tmp.delete()
        throw ex
    }
}

/**
 * Load (or regenerate on corruption) the BKS keystore used by the HTTPS connector.
 */
private fun getSslKeyStore(context: Context, password: String): KeyStore {
    val file = File(context.filesDir, Constants.KEY_STORE_FILE_NAME)
    if (!file.exists()) {
        generateSslKeyStoreFile(file, password)
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
            try {
                generateSslKeyStoreFile(file, password)
                // Reload the newly generated keystore
                file.inputStream().use {
                    load(it, password.toCharArray())
                }
            } catch (ex2: Exception) {
                LogCat.e("Failed to regenerate keystore: ${ex2.message}")
                ex2.printStackTrace()
                throw ex2
            }
        }
    }
}

/**
 * Create and configure the Ktor/Netty embedded server with HTTP+HTTPS connectors.
 * Does not start the server; caller is responsible for calling `start(wait = false)`.
 */
suspend fun createHttpServerAsync(context: Context): EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration> {
    val password = KeyStorePasswordPreference.getAsync()
    return withIO {
        val passwordArray = password.toCharArray()
        val httpPort = TempData.httpPort.value
        val httpsPort = TempData.httpsPort.value
        val environment = applicationEnvironment {
            log = LoggerFactory.getLogger("ktor.application")
        }

        embeddedServer(Netty, environment, configure = {
            // Ktor's default is 32 requests per HTTP pipeline. Allowing 1,000
            // lets a single browser connection overwhelm a memory-constrained
            // Android compatibility container during repeated API calls.
            runningLimit = 32
            tcpKeepAlive = true
            enableHttp2 = false

            connector {
                port = httpPort
            }
            sslConnector(
                keyStore = getSslKeyStore(context, password),
                keyAlias = SSL_KEY_ALIAS,
                keyStorePassword = { passwordArray },
                privateKeyPassword = { passwordArray },
            ) {
                port = httpsPort
            }
        }, HttpModule.module)
    }
}

/**
 * Return the raw DER signature bytes of the HTTPS certificate, used by the
 * web UI to display the certificate fingerprint for trust verification.
 */
fun getSslSignatureBytes(context: Context, password: String): ByteArray {
    val keystore = getSslKeyStore(context, password)
    val cert = keystore.getCertificate(SSL_KEY_ALIAS) as X509Certificate
    return cert.signature
}
