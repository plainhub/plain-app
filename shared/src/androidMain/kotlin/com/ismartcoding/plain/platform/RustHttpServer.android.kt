package com.ismartcoding.plain.platform

import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.httpserver.http.HttpCall
import com.ismartcoding.plain.lib.extensions.getContentType
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.preferences.SystemPrefs
import kotlin.io.encoding.Base64

internal fun decodeRustTlsPkcs12(bytes: ByteArray, password: String): Pair<String, String> {
    val store = java.security.KeyStore.getInstance("PKCS12").apply {
        bytes.inputStream().use { load(it, password.toCharArray()) }
    }
    val alias = store.aliases().asSequence().firstOrNull { store.isKeyEntry(it) }
        ?: error("No private key found in certificate file")
    val chain = checkNotNull(store.getCertificateChain(alias))
    val cert = chain.joinToString("\n") { "-----BEGIN CERTIFICATE-----\n${Base64.encode(it.encoded)}\n-----END CERTIFICATE-----" }
    val key = store.getKey(alias, password.toCharArray())
    return cert to "-----BEGIN PRIVATE KEY-----\n${Base64.encode(key.encoded)}\n-----END PRIVATE KEY-----"
}
internal actual suspend fun serveRustWebAsset(call: HttpCall): Boolean = withIO {
    val requested = java.net.URLDecoder.decode(call.path.removePrefix("/"), "UTF-8")
    if (requested.split('/').any { it == ".." || it == "." } || requested.contains('\\')) return@withIO false
    val resource = requested.ifEmpty { "index.html" }
    val direct = appContext.classLoader.getResourceAsStream("web/$resource")
    val spa = !resource.contains('.')
    val stream = direct ?: if (spa) appContext.classLoader.getResourceAsStream("web/index.html") else null
    stream ?: return@withIO false
    call.responseHeader("Cache-Control", if (requested.startsWith("assets/")) "public, max-age=31536000" else "no-cache, no-store")
    stream.use {
        if (resource == "index.html" || direct == null) {
            val html = it.bufferedReader().readText().replace("<head>", "<head><script>window.__SERVER_TIME__=${System.currentTimeMillis()}</script>")
            call.respondText(html, "text/html; charset=utf-8")
        } else {
            call.respondStream(resource.getContentType().toString()) { sink ->
                val buffer = ByteArray(64 * 1024)
                while (true) { val length = it.read(buffer); if (length < 0) break; sink.write(buffer, 0, length) }
            }
        }
    }
    true
}
