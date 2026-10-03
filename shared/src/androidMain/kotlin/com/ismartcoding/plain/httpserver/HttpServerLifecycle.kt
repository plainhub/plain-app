package com.ismartcoding.plain.httpserver

import com.ismartcoding.plain.preferences.*

import android.content.Context
import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.lib.coIO
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.platform.createHttpClient
import com.ismartcoding.plain.lib.ktorserver.core.engine.EmbeddedServer
import com.ismartcoding.plain.lib.ktorserver.core.engine.applicationEnvironment
import com.ismartcoding.plain.lib.ktorserver.core.engine.connector
import com.ismartcoding.plain.lib.ktorserver.core.engine.embeddedServer
import com.ismartcoding.plain.lib.ktorserver.core.engine.sslConnector
import com.ismartcoding.plain.lib.ktorserver.Netty
import com.ismartcoding.plain.lib.ktorserver.NettyApplicationEngine
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.security.KeyFactory
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64

/**
 * The live Ktor/Netty embedded server instance, or null when the server is stopped.
 * Platform lifecycle code below owns this reference; business state lives in
 * [HttpServerManager] (commonMain).
 */
@Volatile
var httpServer: EmbeddedServer<*, *>? = null

private val SSL_KEY_ALIAS = Constants.SSL_NAME

fun warmUpHttpServer() {
    coIO {
        if (UserPrefs.service.value) return@coIO
        try {
            HttpRouteRegistry.mainGraphQL
            HttpRouteRegistry.peerGraphQL
            HttpRouteRegistry.guestGraphQL
            HttpRouteRegistry.router
            LogCat.d("Route registry warm-up complete")
        } catch (ex: Exception) {
            LogCat.e("Route registry warm-up failed: ${ex.message}")
        }
        try {
            createHttpClient().close()
        } catch (_: Exception) {
        }

    }
}

/**
 * Replace the HTTPS keystore with a user-provided PKCS#12 (.p12/.pfx) bundle.
 *
 * Loads the bundle with [p12Password], extracts the first private-key entry,
 * and re-stores it as a platform PKCS#12 keystore under the app's own alias
 * ([SSL_KEY_ALIAS]) and [keystorePassword] using an atomic write (see
 * [storeSslKeyStore]).
 *
 * @return the raw signature bytes of the newly installed certificate
 * @throws Exception when the bundle can't be parsed, the password is wrong, or
 *         no private key / certificate is found
 */
fun replaceSslKeyStoreBytes(file: File, p12Bytes: ByteArray, p12Password: String, keystorePassword: String): ByteArray {
    val p12 = KeyStore.getInstance("PKCS12").apply { ByteArrayInputStream(p12Bytes).use { load(it, p12Password.toCharArray()) } }
    val alias = p12.aliases().asSequence().firstOrNull { p12.isKeyEntry(it) }
        ?: throw IllegalStateException("No private key found in the certificate file")
    val key = p12.getKey(alias, p12Password.toCharArray()) as? PrivateKey
        ?: throw IllegalStateException("No private key found in the certificate file")
    val chain = p12.getCertificateChain(alias)?.takeIf { it.isNotEmpty() }
        ?: p12.getCertificate(alias)?.let { arrayOf(it) }
        ?: throw IllegalStateException("No certificate found in the certificate file")
    return storeSslKeyStore(file, key, chain, keystorePassword)
}

/**
 * Replace the HTTPS keystore with a user-provided PEM certificate + private key pair.
 *
 * @return the raw signature bytes of the newly installed certificate
 * @throws Exception when either PEM file is malformed
 */
fun replaceSslKeyStoreFromPem(file: File, certPem: String, keyPem: String, keystorePassword: String): ByteArray {
    val cert = parsePemCertificate(certPem)
    val key = parsePemPrivateKey(keyPem)
    return storeSslKeyStore(file, key, arrayOf(cert), keystorePassword)
}

/**
 * Store [key] + [chain] into a fresh platform PKCS#12 keystore at [file] (atomic
 * write) under the app's own alias and password, then return the certificate's
 * signature bytes.
 */
private fun storeSslKeyStore(file: File, key: PrivateKey, chain: Array<Certificate>, keystorePassword: String): ByteArray {
    cachedKeyStore = null
    val keystore = KeyStore.getInstance("PKCS12").apply { load(null, null) }
    keystore.setKeyEntry(SSL_KEY_ALIAS, key, keystorePassword.toCharArray(), chain)
    val tmp = File(file.parent, "${file.name}.tmp")
    try {
        FileOutputStream(tmp).use { keystore.store(it, keystorePassword.toCharArray()) }
        tmp.renameTo(file)
    } catch (ex: Exception) {
        tmp.delete()
        throw ex
    }
    val cert = keystore.getCertificate(SSL_KEY_ALIAS) as X509Certificate
    return cert.signature
}

/** Strip the `-----BEGIN X-----`/`-----END X-----` armor and Base64-decode the body. */
private fun decodePemBlock(pem: String, label: String): ByteArray {
    val begin = "-----BEGIN $label-----"
    val end = "-----END $label-----"
    val start = pem.indexOf(begin).takeIf { it >= 0 }
        ?: throw IllegalStateException("No $label block found in the PEM data")
    val bodyStart = start + begin.length
    val endIndex = pem.indexOf(end, bodyStart)
        ?: throw IllegalStateException("Malformed $label block in the PEM data")
    val body = pem.substring(bodyStart, endIndex).replace(Regex("\\s"), "")
    return Base64.getDecoder().decode(body)
}

private fun parsePemCertificate(pem: String): X509Certificate {
    val der = decodePemBlock(pem, "CERTIFICATE")
    return CertificateFactory.getInstance("X.509").generateCertificate(ByteArrayInputStream(der)) as X509Certificate
}

/**
 * Parse a PEM-encoded EC private key, PKCS#8 (`BEGIN PRIVATE KEY`) only.
 * SEC1 (`BEGIN EC PRIVATE KEY`) is not supported and fails with a clear error.
 */
private fun parsePemPrivateKey(pem: String): PrivateKey {
    val der = decodePemBlock(pem, "PRIVATE KEY")
    val keyFactory = KeyFactory.getInstance("EC")
    return keyFactory.generatePrivate(PKCS8EncodedKeySpec(der))
}

/** Last keystore parsed by [getSslKeyStore], with the password it was opened with. */
@Volatile
private var cachedKeyStore: Pair<String, KeyStore>? = null

@Synchronized
internal fun getSslKeyStore(context: Context, password: String): KeyStore {
    cachedKeyStore?.let { (cachedPassword, keyStore) -> if (cachedPassword == password) return keyStore }
    val store = KeyStore.getInstance("PKCS12").apply {
        File(context.filesDir, Constants.KEY_STORE_FILE_NAME).inputStream().use { load(it, password.toCharArray()) }
    }
    cachedKeyStore = password to store
    return store
}

/**
 * Create and configure the Ktor/Netty embedded server with HTTP+HTTPS connectors.
 * Does not start the server; caller is responsible for calling `start(wait = false)`.
 */
suspend fun createHttpServerAsync(context: Context): EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration> {
    val t0 = System.currentTimeMillis()
    val password = SystemPrefs.keyStorePassword.value
    return withIO {
        val t1 = System.currentTimeMillis()
        val passwordArray = password.toCharArray()
        val httpPort = UserPrefs.httpPort.value
        val httpsPort = UserPrefs.httpsPort.value
        val keyStore = getSslKeyStore(context, password)
        val t2 = System.currentTimeMillis()
        val environment = applicationEnvironment {
            log = LoggerFactory.getLogger("ktor.application")
        }

        embeddedServer(Netty, environment, configure = {
            // Ktor's default is 32 requests per HTTP pipeline. Allowing 1,000
            // lets a single browser connection overwhelm a memory-constrained
            // Android compatibility container during repeated API calls.
            runningLimit = 32
            tcpKeepAlive = true

            // Bind on all interfaces (0.0.0.0): local/loopback access (health
            // checks, the desktop web console) arrives directly, and LAN devices
            // reach the server through the Wi-Fi address.
            connector {
                port = httpPort
            }
            sslConnector(
                keyStore = keyStore,
                keyAlias = SSL_KEY_ALIAS,
                keyStorePassword = { passwordArray },
                privateKeyPassword = { passwordArray },
            ) {
                port = httpsPort
            }
        }, HttpModule.module)
        .also { LogCat.d("createHttpServer: pref=${t1 - t0}ms keystore=${t2 - t1}ms server=${System.currentTimeMillis() - t2}ms") }
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
