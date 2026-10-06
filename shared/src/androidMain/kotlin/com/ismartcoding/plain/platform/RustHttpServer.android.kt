package com.ismartcoding.plain.platform

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
