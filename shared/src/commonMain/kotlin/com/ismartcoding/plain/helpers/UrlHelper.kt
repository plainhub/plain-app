package com.ismartcoding.plain.helpers

import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.lib.extensions.getFilenameExtension
import com.ismartcoding.plain.platform.chaCha20Decrypt
import com.ismartcoding.plain.platform.chaCha20Encrypt
import com.ismartcoding.plain.platform.getDeviceIP4
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
object UrlHelper {
    private val mediaPathMap = mutableMapOf<String, String>() // format: <short_path>:<raw_path>

    fun getMediaHttpUrl(path: String): String {
        val id = TimeHelper.nowMillis().toString()
        mediaPathMap[id] = path
        val extension = path.getFilenameExtension()
        return "http://${getDeviceIP4()}:${TempData.httpPort.value}/media/$id.$extension"
    }

    fun getAlbumArtHttpUrl(albumUri: String): String {
        val id = "art_${TimeHelper.nowMillis()}"
        mediaPathMap[id] = albumUri
        return "http://${getDeviceIP4()}:${TempData.httpPort.value}/media/$id.jpg"
    }

    fun getCastCallbackUrl(): String {
        return "http://${getDeviceIP4()}:${TempData.httpPort.value}/callback/cast"
    }

    fun getHealthCheckUrl(): String {
        return "http://127.0.0.1:${TempData.httpPort.value}/health"
    }

    fun getShutdownUrl(): String {
        return "http://127.0.0.1:${TempData.httpPort.value}/shutdown"
    }

    fun getMediaPath(id: String): String {
        return mediaPathMap[id] ?: ""
    }

    fun encrypt(path: String): String {
        val bytes = chaCha20Encrypt(TempData.urlToken, path)
        return Base64.encode(bytes)
    }

    fun decrypt(id: String): String {
        val bytes = Base64Lenient.decode(id)
        return chaCha20Decrypt(TempData.urlToken, bytes)?.decodeToString() ?: ""
    }

    fun getPolicyUrl(): String {
        return "https://plainhub.github.io/plain-app/policy.html"
    }

    fun getTermsUrl(): String {
        return "https://plainhub.github.io/plain-app/terms.html"
    }
}
