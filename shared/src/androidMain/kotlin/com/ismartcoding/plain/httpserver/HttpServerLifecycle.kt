package com.ismartcoding.plain.httpserver

import android.content.Context
import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.lib.apk.cert.x509.X509SelfSignedGenerator
import com.ismartcoding.plain.lib.coIO
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.preferences.KeyStorePasswordPreference
import com.ismartcoding.plain.lib.ktorserver.PlainNettyServer
import com.ismartcoding.plain.lib.ktorserver.core.engine.connector
import com.ismartcoding.plain.lib.ktorserver.core.engine.sslConnector
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
 * The live Netty server instance, or null when the server is stopped.
 * Platform lifecycle code below owns this reference; business state lives in
 * [HttpServerManager] (commonMain).
 */
@Volatile
var httpServer: PlainNettyServer? = null

private val SSL_KEY_ALIAS = Constants.SSL_NAME

/**
 * Start a throwaway Netty server to preload classes/JIT on app launch so the
 * first real server start is fast.
 */
fun warmUpNetty() {
    coIO {
        try {
            val s = PlainNettyServer(
                requestHandler = {},
            ) {
                connector {
                    port = 0
                    host = "127.0.0.1"
                }
            }.start(wait = false)
            s.stop(0, 0)
            LogCat.d("Netty warm-up complete")
        } catch (_: Exception) {
        }
    }
}

/**
 * Generate a fresh PKCS#12 keystore file and atomically replace [file].
 */
fun generateSslKeyStoreFile(file: File, password: String) {
    val keyStore = X509SelfSignedGenerator.newSelfSignedKeyStore(SSL_KEY_ALIAS, password, Constants.SSL_NAME)
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

/**
 * Load (or regenerate on corruption) the platform PKCS#12 keystore used by the HTTPS connector.
 */
private fun getSslKeyStore(context: Context, password: String): KeyStore {
    val file = File(context.filesDir, Constants.KEY_STORE_FILE_NAME)
    if (!file.exists()) {
        generateSslKeyStoreFile(file, password)
    }

    return KeyStore.getInstance("PKCS12").apply {
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
suspend fun createHttpServerAsync(context: Context): PlainNettyServer {
    val password = KeyStorePasswordPreference.getAsync()
    return withIO {
        val passwordArray = password.toCharArray()
        val httpPort = TempData.httpPort.value
        val httpsPort = TempData.httpsPort.value

        PlainNettyServer(
            requestHandler = PlainHttpServer.requestHandler,
        ) {
            log = LoggerFactory.getLogger("ktor.application")
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
                keyStore = getSslKeyStore(context, password),
                keyAlias = SSL_KEY_ALIAS,
                keyStorePassword = { passwordArray },
                privateKeyPassword = { passwordArray },
            ) {
                port = httpsPort
            }
        }
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
