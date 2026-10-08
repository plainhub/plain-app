package com.ismartcoding.plain.helpers

import com.ismartcoding.plain.preferences.*
import kotlinx.serialization.json.jsonPrimitive

import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.platform.chaCha20Decrypt
import com.ismartcoding.plain.platform.chaCha20Encrypt
import com.ismartcoding.plain.platform.getDeviceIP4
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
object UrlHelper {
    /**
     * Build `scheme://host[:port][/path]`. The port is omitted when it is the
     * scheme default (http: 80, https/wss: 443).
     */
    fun buildUrl(scheme: String, host: String, port: Int, path: String = ""): String {
        val portPart = if ((scheme == "http" && port == 80) || (scheme == "https" || scheme == "wss") && port == 443) "" else ":$port"
        return "$scheme://$host$portPart$path"
    }

    fun getHealthCheckUrl(): String {
        return buildUrl("http", "127.0.0.1", UserPrefs.httpPort.value, "/health")
    }

    fun getShutdownUrl(): String {
        return buildUrl("http", "127.0.0.1", UserPrefs.httpPort.value, "/shutdown")
    }

    fun encrypt(path: String): String {
        return encrypt(path, TempData.urlToken)
    }

    fun decrypt(id: String): String {
        return decrypt(id, TempData.urlToken)
    }

    /** Encrypt [text] with a caller-supplied [key]; used by shared links (per-share key). */
    @OptIn(ExperimentalEncodingApi::class)
    fun encrypt(text: String, key: ByteArray): String {
        val bytes = chaCha20Encrypt(key, text)
        return Base64.encode(bytes)
    }

    /** Decrypt [id] with a caller-supplied [key]; used by shared links (per-share key). */
    @OptIn(ExperimentalEncodingApi::class)
    fun decrypt(id: String, key: ByteArray): String {
        val bytes = Base64Lenient.decode(id)
        return chaCha20Decrypt(key, bytes)?.decodeToString() ?: ""
    }

    fun getPolicyUrl(): String {
        return "https://plainapp.app/privacy"
    }

    fun getTermsUrl(): String {
        return "https://plainapp.app/terms"
    }
}
