package com.ismartcoding.plain.helpers

import android.net.Uri
import android.util.Base64
import com.ismartcoding.plain.lib.extensions.getFilenameExtension
import com.ismartcoding.plain.lib.helpers.CryptoHelper
import com.ismartcoding.plain.lib.helpers.NetworkHelper
import com.ismartcoding.plain.TempData

object UrlHelper {
    private val mediaPathMap = mutableMapOf<String, String>() // format: <short_path>:<raw_path>

    fun getMediaHttpUrl(path: String): String {
        // cast screen only only supports http in local network and some TV OS only supports simple file name with extension
        val id = System.currentTimeMillis().toString()
        mediaPathMap[id] = path
        val extension = path.getFilenameExtension()
        return "http://${NetworkHelper.getDeviceIP4()}:${TempData.httpPort.value}/media/$id.$extension"
    }

    fun getAlbumArtHttpUrl(albumUri: Uri): String {
        val id = "art_${System.currentTimeMillis()}"
        mediaPathMap[id] = albumUri.toString()
        return "http://${NetworkHelper.getDeviceIP4()}:${TempData.httpPort.value}/media/$id.jpg"
    }

    fun getCastCallbackUrl(): String {
        return "http://${NetworkHelper.getDeviceIP4()}:${TempData.httpPort.value}/callback/cast"
    }

    fun getHealthCheckUrl(): String {
        // Use 127.0.0.1 instead of localhost to skip DNS resolution — on some
        // Android ROMs localhost lookup blocks or fails, which races with the
        // health check and produces a false "Connection refused" right after
        // Ktor's `start(wait = false)` returns (port not yet bound).
        return "http://127.0.0.1:${TempData.httpPort.value}/health"
    }

    fun getShutdownUrl(): String {
        return "http://127.0.0.1:${TempData.httpPort.value}/shutdown"
    }

    fun getMediaPath(id: String): String {
        return mediaPathMap[id] ?: ""
    }

    fun decrypt(id: String): String {
        val bytes = Base64.decode(id, Base64.NO_WRAP)
        return CryptoHelper.chaCha20Decrypt(TempData.urlToken, bytes)?.decodeToString() ?: ""
    }

    fun getPolicyUrl(): String {
        return "https://plainhub.github.io/plain-app/policy.html"
    }

    fun getTermsUrl(): String {
        return "https://plainhub.github.io/plain-app/terms.html"
    }
}
