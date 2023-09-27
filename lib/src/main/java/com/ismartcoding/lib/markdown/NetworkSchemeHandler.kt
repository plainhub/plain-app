package com.ismartcoding.lib.markdown

import android.net.Uri
import io.noties.markwon.image.ImageItem
import io.noties.markwon.image.SchemeHandler
import java.io.BufferedInputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class NetworkSchemeHandler() : SchemeHandler() {
    override fun handle(
        raw: String,
        uri: Uri,
    ): ImageItem {
        val imageItem: ImageItem
        try {
            val url = URL(raw)
            val connection = url.openConnection() as HttpURLConnection
            connection.connect()

            val responseCode = connection.responseCode
            imageItem =
                when (responseCode) {
                    in 200..299 -> {
                        val contentType = contentType(connection.getHeaderField("Content-Type"))
                        val inputStream = BufferedInputStream(connection.inputStream)
                        ImageItem.withDecodingNeeded(contentType, inputStream)
                    }
                    HttpURLConnection.HTTP_MOVED_PERM, HttpURLConnection.HTTP_MOVED_TEMP, HttpURLConnection.HTTP_SEE_OTHER -> {
                        val redirectUrl = connection.getHeaderField("Location")
                        handle(redirectUrl, uri)
                    }
                    else -> {
                        throw IOException("Bad response code: $responseCode, url: $raw")
                    }
                }
        } catch (e: IOException) {
            throw IllegalStateException("Exception obtaining network resource: $raw", e)
        }
        return imageItem
    }

    override fun supportedSchemes(): Collection<String> {
        return listOf(SCHEME_HTTP, SCHEME_HTTPS)
    }

    private fun contentType(contentType: String?): String? {
        if (contentType == null) {
            return null
        }
        val index = contentType.indexOf(';')
        return if (index > -1) {
            contentType.substring(0, index)
        } else {
            contentType
        }
    }

    companion object {
        const val SCHEME_HTTP = "http"
        const val SCHEME_HTTPS = "https"
    }
}
